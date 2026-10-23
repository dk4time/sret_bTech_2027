/*
 * Program 5.4 - Count vowels, consonants and words
 * Efftronics Placement Preparation - C Programming Handout
 * Dineshkumar, Subject Matter Expert, T.I.M.E.
 */

#include <stdio.h>

int main(void) {
    char s[] = "Efftronics builds smart IoT products";
    int vowels = 0, consonants = 0, words = 0, inWord = 0;

    for (int i = 0; s[i] != '\0'; i++) {
        char c = s[i];
        if (c >= 'A' && c <= 'Z') c += 32;          /* to lowercase */
        if (c >= 'a' && c <= 'z') {
            if (c == 'a' || c == 'e' || c == 'i' || c == 'o' || c == 'u')
                vowels++;
            else
                consonants++;
        }
        if (s[i] != ' ' && !inWord) { words++; inWord = 1; }
        else if (s[i] == ' ')       inWord = 0;
    }
    printf("Vowels = %d, Consonants = %d, Words = %d\n",
           vowels, consonants, words);
    return 0;
}
