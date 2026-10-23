/*
 * Program 12.1 - Mixed traps
 * Efftronics Placement Preparation - C Programming Handout
 * Dineshkumar, Subject Matter Expert, T.I.M.E.
 */

#include <stdio.h>

int main(void) {
    int n = printf("Hello\n");            /* printf returns chars printed */
    printf("n = %d\n", n);

    int i = 5;
    int j = i++ + 2;                      /* uses 5, then i becomes 6 */
    int k = ++i * 2;                      /* i becomes 7, then 7 * 2  */
    printf("i = %d, j = %d, k = %d\n", i, j, k);

    unsigned char u = 255;
    u++;                                  /* wraps around */
    printf("u = %d\n", u);

    printf("sizeof('A') = %zu\n", sizeof('A'));   /* a char literal is an int in C */

    float f = 0.1f;
    printf("%s\n", f == 0.1 ? "equal" : "not equal");

    int x;
    for (x = 0; x < 3; x++);              /* note the semicolon */
    printf("x = %d\n", x);

    switch (2) {
        case 1: printf("one ");
        case 2: printf("two ");
        case 3: printf("three ");         /* no break: falls through */
    }
    printf("\n");
    return 0;
}
