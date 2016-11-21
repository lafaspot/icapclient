#!/usr/bin/env bash
if [ "$TRAVIS_BRANCH" = 'master' ] && [ "$TRAVIS_PULL_REQUEST" == 'false' ]; then
    openssl aes-256-cbc -K $encrypted_08cae0b40b89_key -iv $encrypted_08cae0b40b89_iv -in codesigning.asc.enc -out signingkey.asc -d
    gpg --fast-import cd/signingkey.asc
fi
