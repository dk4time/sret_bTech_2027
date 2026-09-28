package com.ridehailing.model.payment;

import com.ridehailing.exception.RideHailingException;
import com.ridehailing.model.common.Money;

/**
 * ABSTRACTION through an INTERFACE: "something that can pay an amount".
 * UpiPayment, CashPayment and WalletPayment each implement pay() their own way.
 *
 * DYNAMIC DISPATCH: PaymentService holds a Payable reference and calls pay(); Java picks
 * the implementation at runtime from the real object.
 *
 * pay() declares the base RideHailingException because some method may refuse (a wallet
 * with too little money). An implementation may declare FEWER exceptions: WalletPayment
 * narrows it to InsufficientWalletBalanceException, UpiPayment and CashPayment declare none.
 */
public interface Payable {

    PaymentStatus pay(Money amount) throws RideHailingException;

    PaymentMethod method();

    PaymentStatus status();

    /** Human-readable result of the attempt, e.g. a UPI reference or a failure reason. */
    String resultMessage();
}
