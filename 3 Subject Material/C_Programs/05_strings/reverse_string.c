/*
 * Program 5.2 - Reverse a string in place
 * Efftronics Placement Preparation - C Programming Handout
 * Dineshkumar, Subject Matter Expert, T.I.M.E.
 */

#include <stdio.h>

void reverseStr(char s[]) {
    int r = 0;
    while (s[r] != '\0') r++;
    r--;
    for (int l = 0; l < r; l++, r--) {
        char t = s[l]; s[l] = s[r]; s[r] = t;
    }
}

int main(void) {
    char s[] = "embedded";
    reverseStr(s);
    printf("%s\n", s);
    return 0;
}
