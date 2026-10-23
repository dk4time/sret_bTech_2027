/*
 * Program 5.3 - Palindrome string
 * Efftronics Placement Preparation - C Programming Handout
 * Dineshkumar, Subject Matter Expert, T.I.M.E.
 */

#include <stdio.h>

int isPalindrome(const char s[]) {
    int r = 0;
    while (s[r] != '\0') r++;
    for (int l = 0, k = r - 1; l < k; l++, k--)
        if (s[l] != s[k]) return 0;
    return 1;
}

int main(void) {
    printf("madam : %s\n", isPalindrome("madam") ? "yes" : "no");
    printf("level : %s\n", isPalindrome("level") ? "yes" : "no");
    printf("hello : %s\n", isPalindrome("hello") ? "yes" : "no");
    return 0;
}
