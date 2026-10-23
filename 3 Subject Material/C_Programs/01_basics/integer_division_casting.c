/*
 * Program 1.2 - Integer division and type casting
 * Efftronics Placement Preparation - C Programming Handout
 * Dineshkumar, Subject Matter Expert, T.I.M.E.
 */

#include <stdio.h>

int main(void) {
    int a = 7, b = 2;
    char c = 'A';

    printf("%d\n", a / b);              /* 3     */
    printf("%d\n", a % b);              /* 1     */
    printf("%.2f\n", (float)a / b);     /* 3.50  */
    printf("%.2f\n", (float)(a / b));   /* 3.00  */
    printf("%d %c\n", c, c + 2);        /* 65 C  */
    return 0;
}
