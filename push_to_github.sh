#!/data/data/com.termux/files/usr/bin/bash
set -e

cd /data/data/com.termux/files/home/PreetsDetoxLauncher

TOKEN="$1"

echo "=========================================================="
echo "  Deploying Preet's Detox Launcher to GitHub"
echo "  Repository: https://github.com/PreetTank35/PreetsDetoxLauncher"
echo "=========================================================="

# Configure Git Identity
if [ -z "$(git config user.email)" ]; then
    git config user.email "preettank35@users.noreply.github.com"
    git config user.name "Preet Tank"
fi

# Ensure main branch is active
git branch -M main 2>/dev/null || true

# Check if commit exists
if ! git rev-parse --verify HEAD >/dev/null 2>&1; then
    echo "[*] Staging and creating initial commit..."
    git add -A
    git commit -m "feat: complete native android implementation for Preet's Detox Launcher"
else
    git add -A
    git commit -m "feat: update Preet's Detox Launcher" 2>/dev/null || echo "[*] Working tree clean, commit ready."
fi

# Push
if [ -n "$TOKEN" ]; then
    echo "[*] Pushing to GitHub using provided token..."
    git push "https://PreetTank35:${TOKEN}@github.com/PreetTank35/PreetsDetoxLauncher.git" main --force
else
    echo "[*] Setting remote origin..."
    git remote remove origin 2>/dev/null || true
    git remote add origin https://github.com/PreetTank35/PreetsDetoxLauncher.git
    echo "[*] Pushing to GitHub main branch..."
    echo "Tip: Termux hides passwords completely while typing for security."
    git push -u origin main --force
fi

echo ""
echo "=========================================================="
echo "  SUCCESSFULLY PUSHED TO GITHUB!"
echo "  GitHub Actions CI/CD has now been triggered."
echo "  Watch the build and download your APK at:"
echo "  https://github.com/PreetTank35/PreetsDetoxLauncher/actions"
echo "=========================================================="
