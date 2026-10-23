/*
 * Program 5.5 - Check whether two strings are anagrams
 * Efftronics Placement Preparation - C Programming Handout
 * Dineshkumar, Subject Matter Expert, T.I.M.E.
 */

#include <stdio.h>

int isAnagram(const char *a, const char *b) {
    int count[26] = {0};
    for (int i = 0; a[i]; i++) count[a[i] - 'a']++;
    for (int i = 0; b[i]; i++) count[b[i] - 'a']--;
    for (int i = 0; i < 26; i++)
        if (count[i] != 0) return 0;
    return 1;
}

int main(void) {
    printf("listen / silent : %s\n", isAnagram("listen", "silent") ? "yes" : "no");
    printf("hello  / world  : %s\n", isAnagram("hello", "world") ? "yes" : "no");
    return 0;
}
