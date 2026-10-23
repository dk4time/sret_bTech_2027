/*
 * Program 1.3 - Precedence and short-circuit evaluation
 * Efftronics Placement Preparation - C Programming Handout
 * Dineshkumar, Subject Matter Expert, T.I.M.E.
 */

#include <stdio.h>

int main(void) {
    int x = 5, y = 10;

    printf("%d\n", x + y * 2);          /* 25 */
    printf("%d\n", (x + y) * 2);        /* 30 */
    printf("%d\n", x > 3 && y < 5);     /* 0  */
    printf("%d\n", x > 3 || y++);       /* 1  */
    printf("y = %d\n", y);              /* y is still 10 */
    return 0;
}
