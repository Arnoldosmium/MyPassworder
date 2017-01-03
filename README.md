# My Passworder
**A desktop Java software that helps create and keep complicated and flexible passwords**

As more and more business runs online, modern people have to deal with many accounts like never before. The cyber security issues (like millons of passwords got exposure to hackers) that happened in recent years emphasize more complex passwords that should differ from site to site. However, the most (well, including myself) sometimes are frustrated by creating and then memorizing complicated passwords; putting it down is also unsafe, since the local files could be hacked as well. Is there any way to solve this dilemma?

The key idea is: people are more likely to retrieve memory if a "*clue word*" is provided. My passworder is responsible to remember "clue words", or **PassPhrase** in this software, e.g. my birthday, the brand of my first car, the date I met her... For sure you can also put password here; yet it is discouraged due to security reasons, apparently. When the password slipped away from your mind, simply click on the website, and MyPassword will provide the clue words to help reconstruct the password.

Creating new passwords might also be frustrating. Random passwords are hard to remember; some sequences in the current password are not allowed to use. What about combine the "clue words" together?! The nature of *permutation* can created a huge amount of passwords that seldomly repretitive, yet easy to remember.  

So that's the main approach of MyPassworder: manage the password in blocks of words, permute them randomly to create passwords, and save them for reference at any time.

## Key Concepts:
- **PassPhrases**: A clue word that represents a part in the password. Like: the year of birth, the brand of my first car, etc.
- **PassPhrase Groups**: A group of PassPhrases. You can organize a group semantically or in terms of positions.
- **Password Structure**: Formed by probability and groups of PassPhrases; a guidance to from a complicated yet rememberable password.

## What is used:
- **Java Swing**: My first Swing application. A lot to improve.
- **SQLite**: An efficient way to manipulate small amount of data.

## Furture Improvement:
- Well, Documentation (70%)
- Improve Password Structure Editor. (GridBagLayout is not that easy to use)
- Start the application at one click. (On-going)

Last bit of code in 2016! (And the first bit of code in 2017)
