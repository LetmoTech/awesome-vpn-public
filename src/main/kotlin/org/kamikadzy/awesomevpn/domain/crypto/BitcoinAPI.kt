package org.kamikadzy.awesomevpn.domain.crypto

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bitcoinj.core.Address
import org.bitcoinj.core.Coin
import org.bitcoinj.core.NetworkParameters
import org.bitcoinj.kits.WalletAppKit
import org.bitcoinj.params.MainNetParams
import org.bitcoinj.script.Script
import org.bitcoinj.utils.BriefLogFormatter
import org.bitcoinj.wallet.DeterministicSeed
import org.bitcoinj.wallet.SendRequest
import org.kamikadzy.awesomevpn.db.user.CryptoCurrencies
import org.kamikadzy.awesomevpn.db.user.UserService
import org.kamikadzy.awesomevpn.domain.api.BlockCypherAPI
import org.kamikadzy.awesomevpn.utils.BitcoinBigDecimal
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.io.File
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.prefs.Preferences
import kotlin.math.pow

@Component
class BitcoinAPI (
    @Value("\${crypto.bitcoin.seed-phrase}")
    private val seedPhrase: String,
    @Value("\${debug}")
    private val test: String,
    private val blockCypherAPI: BlockCypherAPI,
    private val userService: UserService
): CryptoGateway {
    private val walletAppKit: WalletAppKit

    private final val params: NetworkParameters = MainNetParams.get()

    private val pendingTxs = hashSetOf<String>()

    init {
        val dirBitcoinj = File("bitcoinj")
        if (!dirBitcoinj.exists()) {
            dirBitcoinj.mkdir()
        }

        BriefLogFormatter.initWithSilentBitcoinJ()

        walletAppKit = WalletAppKit(params, Script.ScriptType.P2WPKH, null, dirBitcoinj, "vpn")

        if (test != "true") {
            walletAppKit.setAutoSave(true)
            walletAppKit.startAsync()
            walletAppKit.awaitRunning()

            val seed = DeterministicSeed(seedPhrase, null, "", 0)
            walletAppKit.restoreWalletFromSeed(seed)

            println("seed: $seed")
            println("creation time: " + seed.creationTimeSeconds)


            walletAppKit.wallet().addCoinsReceivedEventListener { _, tx, _, _ ->
                println("Wow! We got transaction ${tx.txId.toString()}")
                val addresses = tx.outputs.map {
                    it.scriptPubKey.getToAddress(params)
                }

                println("Addresses ${addresses}")

                userService.getAllUsers().forEach { user ->
                    if (Address.fromString(params, user.cryptoWallets[CryptoCurrencies.BTC]) in addresses) {
                        val delta = tx.outputs.first { it.scriptPubKey.getToAddress(params).toString() == user.address }.value.value

                        var text = "Баланс кошелька `${user.address}` пополнен на ${
                            BitcoinBigDecimal(delta).divide(BitcoinBigDecimal.TEN.pow(8))
                        }₿."
                        constants.messageSender?.sendMessage(text, user.tgChatId, true)

                        for (administrator in administratorService.getAllAdministrators()) {
                            text = "Баланс кошелька пользователя @${user.tgName} `${user.address}` пополнен на ${
                                BitcoinBigDecimal(delta).divide(BitcoinBigDecimal.TEN.pow(8))
                            }₿."

                            constants.messageSender?.sendMessage(text, administrator.tgChatId, true)
                        }


                        pendingTxs.add(tx.txId.toString())
                    }
                }
            }

            walletAppKit.wallet().addTransactionConfidenceEventListener { _, tx ->
                val toDeleteFromPending = hashSetOf<String>()

                if (tx.txId.toString() in pendingTxs && tx.confidence.depthInBlocks >= 1) {
                    toDeleteFromPending.add(tx.txId.toString())

                    val addresses = tx.outputs.map {
                        it.scriptPubKey.getToAddress(params)
                    }

                    userService.getAllUsers().forEach { user ->
                        if (Address.fromString(params, user.address) in addresses) {
                            val delta = tx.outputs.first {
                                it.scriptPubKey.getToAddress(params).toString() == user.address
                            }.value.value

                            userService.changeUserBalance(
                                userId = user.id!!,
                                delta = delta
                            )

                            Logger.addEntity("Пополнение @${user.tgName} на ${
                                BitcoinBigDecimal(delta).divide(BitcoinBigDecimal.TEN.pow(8))
                            }₿.")

                            val deposit = Deposit(tx.txId.toString(), delta, System.currentTimeMillis(), userId = user.bhKey)
                            depositService.saveDeposit(deposit)

                            userService.updateLimitOnCards(user, true)
                            if (user.balance + delta > constants.getMinimumBalance()) {
                                userService.enableUser(user)
                            }

                            val updatedUser = userService.getUserById(user.id!!)

                            val text = "" +
                                    "\uD83D\uDD25 Ваш кошелек `${updatedUser.address}` пополнен на ${
                                        BitcoinBigDecimal(delta).divide(
                                            BitcoinBigDecimal.TEN.pow(
                                                8
                                            )
                                        )
                                    }₿\n" +
                                    "\n" +
                                    "Баланс: ${updatedUser.getBalanceBTC()}\n" +
                                    "Примерно: ${
                                        rateUpdater.rate.get().multiply(BigDecimal.ONE.add(constants.percent.divide(BigDecimal.TEN.pow(2)))).multiply(updatedUser.getBalanceInBitcoinBigDecimal())
                                            .setScale(2, RoundingMode.CEILING)
                                    } RUB"
                            constants.messageSender?.sendMessage(text, user.tgChatId, true)
                        }
                    }
                }

                pendingTxs.removeAll(toDeleteFromPending)
            }

            print(walletAppKit.wallet().watchedAddresses)
            print(walletAppKit.wallet().unspents)
        }
    }

    fun getMasterBalance(): Long {
        return walletAppKit.wallet().balance.value
    }

    override fun getNewAddress(): String {
        val newKey = walletAppKit.wallet().freshReceiveKey()
        val nnaddress = Address.fromKey(params, newKey, Script.ScriptType.P2WPKH)

        walletAppKit.wallet().addWatchedAddress(nnaddress)

        return nnaddress.toString()
    }

    // amount == null => max
    suspend fun sendFundsOnAddress(address: String, amount: Double?, fee: Long?): String {
        val receiver = Address.fromString(params, address)

        val f = fee ?: blockCypherAPI.getSuggestedFee()

        if (amount == null) {
            val sendRequest = SendRequest.emptyWallet(receiver)
            sendRequest.feePerKb = Coin.valueOf(f)

            val requestResult = walletAppKit.wallet().sendCoins(sendRequest)

            Preferences.userRoot().putDouble(
                "fee",
                Preferences.userRoot().getDouble("fee", 0.0) - requestResult.tx.fee.value / 10.0.pow(8)
            )

            return withContext(Dispatchers.IO) {
                requestResult.broadcastComplete.get()
            }.txId.toString()
        } else {
            val sendRequest = SendRequest.to(receiver, Coin.valueOf((amount * 10.0.pow(8)).toLong()))
            sendRequest.feePerKb = Coin.valueOf(f)

            val sendRequestResult = walletAppKit.wallet().sendCoins(sendRequest)

            Preferences.userRoot().putDouble(
                "fee",
                Preferences.userRoot().getDouble("fee", 0.0) - sendRequestResult.tx.fee.value / 10.0.pow(8)
            )


            return sendRequestResult.broadcastComplete.get().txId.toString()
        }
    }

    fun getWiFPrivateKeys(): List<String> {
        return walletAppKit.wallet().unspents.map {
            val eckey = walletAppKit.wallet().findKeyFromAddress(it.scriptPubKey.getToAddress(params))

            eckey.getPrivateKeyAsWiF(params)
        }
    }
}