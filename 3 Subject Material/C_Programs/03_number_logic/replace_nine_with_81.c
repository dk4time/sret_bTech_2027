/*
 * Program 3.6 - Replace every 9 with 81 (asked at Efftronics)
 * Efftronics Placement Preparation - C Programming Handout
 * Dineshkumar, Subject Matter Expert, T.I.M.E.
 */

#include <stdio.h>

long replaceNine(long n) {
    long result = 0, place = 1;
    if (n == 0) return 0;
    while (n > 0) {
        int d = n % 10;
        if (d == 9) {
            result += 81 * place;
            place  *= 100;
        } else {
            result += d * place;
            place  *= 10;
        }
        n /= 10;
    }
    return result;
}

int main(void) {
    printf("%ld\n", replaceNine(1298));
    printf("%ld\n", replaceNine(909));
    return 0;
}
