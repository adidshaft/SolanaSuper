#!/bin/bash

# Configuration
OLD_EMAIL="man22invisible@gmail.com" # Or "johnsoncarl@example.com", depending on the repo
CORRECT_NAME="adidshaft"
CORRECT_EMAIL="adidshaft@gmail.com"

echo "Rewriting history in $(pwd) to change author from $OLD_EMAIL to $CORRECT_NAME <$CORRECT_EMAIL>"

git filter-branch --env-filter '
OLD_EMAIL="'"$OLD_EMAIL"'"
CORRECT_NAME="'"$CORRECT_NAME"'"
CORRECT_EMAIL="'"$CORRECT_EMAIL"'"

if [ "$GIT_COMMITTER_EMAIL" = "$OLD_EMAIL" ]
then
    export GIT_COMMITTER_NAME="$CORRECT_NAME"
    export GIT_COMMITTER_EMAIL="$CORRECT_EMAIL"
fi
if [ "$GIT_AUTHOR_EMAIL" = "$OLD_EMAIL" ]
then
    export GIT_AUTHOR_NAME="$CORRECT_NAME"
    export GIT_AUTHOR_EMAIL="$CORRECT_EMAIL"
fi
' --tag-name-filter cat -- --branches --tags

echo "Done! If the history looks good (run 'git log' to check), you can force push:"
echo "git push --force --tags origin 'refs/heads/*'"
