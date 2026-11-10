#!/data/data/com.termux/files/usr/bin/bash
set -e

cd /data/data/com.termux/files/home/PreetsDetoxLauncher

echo "=========================================================="
echo "  Deploying Preet's Detox Launcher to GitHub"
echo "  Repository: https://github.com/PreetTank35/PreetsDetoxLauncher"
echo "=========================================================="

# 1. Configure Git Identity if not set
if [ -z "$(git config user.email)" ]; then
    echo "[*] Setting local Git author identity..."
    git config user.email "preettank35@users.noreply.github.com"
    git config user.name "Preet Tank"
fi

# 2. Initialize git if not already present
if [ ! -d ".git" ]; then
    echo "[1/4] Initializing Git repository with 'main' branch..."
    git init -b main
else
    echo "[1/4] Ensuring 'main' branch is active..."
    git branch -M main 2>/dev/null || true
fi

# 3. Configure Remote URL
echo "[2/4] Setting remote origin..."
git remote remove origin 2>/dev/null || true
git remote add origin https://github.com/PreetTank35/PreetsDetoxLauncher.git

# 4. Stage & Commit
echo "[3/4] Staging all project files..."
git add -A
git commit -m "feat: complete native android implementation for Preet's Detox Launcher" || echo "Commit already created."

# 5. Push to Remote
echo "[4/4] Pushing to GitHub main branch..."
echo "Note: When prompted for password, enter your GitHub Personal Access Token (PAT)."
git push -u origin main --force

echo ""
echo "=========================================================="
echo "  SUCCESSFULLY PUSHED TO GITHUB!"
echo "  GitHub Actions CI/CD has now been triggered."
echo "  Watch the build and download your APK at:"
echo "  https://github.com/PreetTank35/PreetsDetoxLauncher/actions"
echo "=========================================================="
