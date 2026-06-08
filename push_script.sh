#!/bin/bash
TOKEN=$(gh auth token)
git push "https://x-access-token:${TOKEN}@github.com/osphvdhwj/Redwood-Island-Module.git" HEAD
