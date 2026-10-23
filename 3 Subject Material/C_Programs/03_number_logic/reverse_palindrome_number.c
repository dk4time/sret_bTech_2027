/*
 * Program 3.1 - Reverse a number and check palindrome
 * Efftronics Placement Preparation - C Programming Handout
 * Dineshkumar, Subject Matter Expert, T.I.M.E.
 */

#include <stdio.h>

int reverse(int n) {
    int rev = 0;
    while (n > 0) {
        rev = rev * 10 + n % 10;
        n /= 10;
    }
    return rev;
}

int main(void) {
    int nums[] = {1234, 121, 1001, 10};
    for (int i = 0; i < 4; i++) {
        int n = nums[i];
        printf("%d -> reverse %d -> %s\n", n, reverse(n),
               reverse(n) == n ? "palindrome" : "not palindrome");
    }
    return 0;
}
