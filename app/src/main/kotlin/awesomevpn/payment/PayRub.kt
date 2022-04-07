package awesomevpn.payment

import api.RiseXAPI
import api.advertisement.Advertisement
import api.auth.LoginData
import org.springframework.stereotype.Component

val loginData = LoginData("amaromanov@gmail.com", "gEShgKhB5AbgPag")

@Component
class PayRub : RiseXAPI(loginData) {
    suspend fun pay(amount: Double): Long {
        val bids = getAdvertisements(true) ?: return -1
        for(bid in bids) {
            if (bid != null) {
                if(bid.is_sale == true && bid.is_active == true && bid.min!! <= amount && amount <= bid.max!!) {
                    createDeal(bid, amount)
                }
            }
        }
        return 0
    }
}