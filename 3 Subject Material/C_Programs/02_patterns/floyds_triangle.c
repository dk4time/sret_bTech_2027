/*
 * Program 2.3 - Floyd's triangle
 * Efftronics Placement Preparation - C Programming Handout
 * Dineshkumar, Subject Matter Expert, T.I.M.E.
 */

#include <stdio.h>

int main(void) {
    int n = 4, num = 1;
    for (int i = 1; i <= n; i++) {
        for (int j = 1; j <= i; j++)
            printf("%3d", num++);
        printf("\n");
    }
    return 0;
}
