/*
 * Program 3.9 - Decimal to binary and back (arithmetic only)
 * Efftronics Placement Preparation - C Programming Handout
 * Dineshkumar, Subject Matter Expert, T.I.M.E.
 */

#include <stdio.h>

long toBinary(int n) {
    long bin = 0, place = 1;
    while (n > 0) {
        bin += (n % 2) * place;
        place *= 10;
        n /= 2;
    }
    return bin;
}

int toDecimal(long bin) {
    int dec = 0, base = 1;
    while (bin > 0) {
        dec += (bin % 10) * base;
        base *= 2;
        bin /= 10;
    }
    return dec;
}

int main(void) {
    printf("13 -> %ld\n", toBinary(13));
    printf("1101 -> %d\n", toDecimal(1101));
    return 0;
}
