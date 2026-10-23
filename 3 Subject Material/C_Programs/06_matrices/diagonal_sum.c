/*
 * Program 6.3 - Sum of both diagonals
 * Efftronics Placement Preparation - C Programming Handout
 * Dineshkumar, Subject Matter Expert, T.I.M.E.
 */

#include <stdio.h>

int main(void) {
    int a[3][3] = {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}};
    int n = 3, main = 0, secondary = 0;

    for (int i = 0; i < n; i++) {
        main      += a[i][i];
        secondary += a[i][n - 1 - i];
    }
    printf("Main = %d, Secondary = %d\n", main, secondary);
    return 0;
}
