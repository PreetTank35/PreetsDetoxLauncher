#!/data/data/com.termux/files/usr/bin/bash
set -e

cd /data/data/com.termux/files/home/PreetsDetoxLauncher

TOKEN="$1"

echo "=========================================================="
echo "  Deploying Final Kotlin & CI Fix to Preet's Detox Launcher"
echo "  Repository: https://github.com/PreetTank35/PreetsDetoxLauncher"
echo "=========================================================="

# Configure Git Identity
if [ -z "$(git config user.email)" ]; then
    git config user.email "preettank35@users.noreply.github.com"
    git config user.name "Preet Tank"
fi

git branch -M main 2>/dev/null || true

# Stage all files
git add -A
git commit -m "fix: resolve flow map and wrapper configuration to ensure clean build" || echo "Changes already committed."

# Push
if [ -n "$TOKEN" ]; then
    echo "[*] Pushing with provided token..."
    git push "https://PreetTank35:${TOKEN}@github.com/PreetTank35/PreetsDetoxLauncher.git" main --force
else
    echo "[*] Pushing to GitHub main branch..."
    git push -u origin main --force
fi

echo ""
echo "=========================================================="
echo "  SUCCESSFULLY PUSHED TO GITHUB!"
echo "  Watch the build live at:"
echo "  https://github.com/PreetTank35/PreetsDetoxLauncher/actions"
echo "=========================================================="
