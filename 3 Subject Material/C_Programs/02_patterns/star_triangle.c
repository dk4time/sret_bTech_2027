/*
 * Program 2.1 - Right-angled star triangle
 * Efftronics Placement Preparation - C Programming Handout
 * Dineshkumar, Subject Matter Expert, T.I.M.E.
 */

#include <stdio.h>

int main(void) {
    int n = 4;
    for (int i = 1; i <= n; i++) {
        for (int j = 1; j <= i; j++)
            printf("* ");
        printf("\n");
    }
    return 0;
}
