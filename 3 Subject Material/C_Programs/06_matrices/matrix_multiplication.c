/*
 * Program 6.1 - Matrix multiplication
 * Efftronics Placement Preparation - C Programming Handout
 * Dineshkumar, Subject Matter Expert, T.I.M.E.
 */

#include <stdio.h>

int main(void) {
    int a[2][3] = {{1, 2, 3}, {4, 5, 6}};
    int b[3][2] = {{7, 8}, {9, 10}, {11, 12}};
    int c[2][2] = {{0}};

    for (int i = 0; i < 2; i++)
        for (int j = 0; j < 2; j++)
            for (int k = 0; k < 3; k++)
                c[i][j] += a[i][k] * b[k][j];

    for (int i = 0; i < 2; i++) {
        for (int j = 0; j < 2; j++) printf("%5d", c[i][j]);
        printf("\n");
    }
    return 0;
}
