/*
 * Program 10.3 - Bit-fields
 * Efftronics Placement Preparation - C Programming Handout
 * Dineshkumar, Subject Matter Expert, T.I.M.E.
 */

#include <stdio.h>

struct Flags {
    unsigned int power : 1;    /* 0 or 1  */
    unsigned int mode  : 2;    /* 0 to 3  */
    unsigned int level : 4;    /* 0 to 15 */
};

int main(void) {
    struct Flags f = {1, 3, 9};
    printf("power = %u, mode = %u, level = %u\n", f.power, f.mode, f.level);
    printf("sizeof(struct Flags) = %zu\n", sizeof(struct Flags));
    return 0;
}
