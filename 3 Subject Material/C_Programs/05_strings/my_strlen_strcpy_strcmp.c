/*
 * Program 5.1 - Write strlen, strcpy and strcmp yourself
 * Efftronics Placement Preparation - C Programming Handout
 * Dineshkumar, Subject Matter Expert, T.I.M.E.
 */

#include <stdio.h>

int myStrlen(const char *s) {
    int n = 0;
    while (s[n] != '\0') n++;
    return n;
}

void myStrcpy(char *dst, const char *src) {
    int i = 0;
    while ((dst[i] = src[i]) != '\0') i++;
}

int myStrcmp(const char *a, const char *b) {
    int i = 0;
    while (a[i] != '\0' && a[i] == b[i]) i++;
    return (unsigned char)a[i] - (unsigned char)b[i];
}

int main(void) {
    char copy[20];
    myStrcpy(copy, "Efftronics");
    printf("copy = %s, length = %d\n", copy, myStrlen(copy));
    printf("cmp(apple, apply) = %d\n", myStrcmp("apple", "apply"));
    printf("cmp(abc, abc)     = %d\n", myStrcmp("abc", "abc"));
    return 0;
}
