reCrypt
=======

reCrypt is a simple tool to re encrypt gpg files. It operates on a source directory structure, copying all non `*.gpg` files to a target directory structure, decrypts gpg and encrypt `*.gpg` files. A `SHA-512` message digest is calculated on the decrypted files which is compared to contents of the re encrypted files.  

To build: `./gradlew build`

To run: `java -jar build/libs/recrypt.jar SRC-PATH TARGET-PATH KEY`

It is advisable to warm up `gpg-agent` with all used passphrases, because the program makes no attempt to support asking for passwords.

        uname -a | gpg --encrypt --armor --recipient 0x4938EF36CF7E154C | gpg --decrypt