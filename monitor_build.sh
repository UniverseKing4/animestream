#!/bin/bash
# Auto-monitor and fix GitHub Actions builds

REPO="UniverseKing4/animestream"
MAX_ATTEMPTS=5
attempt=1

echo "🔍 Monitoring build..."

while [ $attempt -le $MAX_ATTEMPTS ]; do
    echo "Attempt $attempt/$MAX_ATTEMPTS"
    
    # Wait for build to start
    sleep 20
    
    # Get latest run
    RUN_ID=$(curl -s "https://api.github.com/repos/$REPO/actions/runs?per_page=1" | python3 -c "import sys, json; data=json.load(sys.stdin); print(data['workflow_runs'][0]['id'])")
    
    # Monitor until complete
    while true; do
        STATUS=$(curl -s "https://api.github.com/repos/$REPO/actions/runs/$RUN_ID" | python3 -c "import sys, json; data=json.load(sys.stdin); print(f\"{data['status']}|{data['conclusion']}\")") 
        
        if echo "$STATUS" | grep -q "completed"; then
            break
        fi
        
        echo "⏳ Build in progress..."
        sleep 30
    done
    
    CONCLUSION=$(echo "$STATUS" | cut -d'|' -f2)
    
    if [ "$CONCLUSION" = "success" ]; then
        echo "✅ BUILD SUCCESS!"
        exit 0
    elif [ "$CONCLUSION" = "failure" ]; then
        echo "❌ BUILD FAILED - Analyzing logs..."
        
        # Get failed steps
        FAILED_STEPS=$(curl -s "https://api.github.com/repos/$REPO/actions/runs/$RUN_ID/jobs" | python3 -c "
import sys, json
data = json.load(sys.stdin)
for job in data.get('jobs', []):
    for step in job.get('steps', []):
        if step.get('conclusion') == 'failure':
            print(step['name'])
")
        
        echo "Failed steps: $FAILED_STEPS"
        
        # Use gh to get detailed logs if available
        if command -v gh &> /dev/null; then
            echo "📋 Fetching detailed logs with gh..."
            cd /root/animestream
            gh run view $RUN_ID --log-failed 2>&1 | head -100 > /tmp/build_error.log
            
            # Auto-fix common errors
            if grep -q "Unresolved reference" /tmp/build_error.log; then
                echo "🔧 Fixing import errors..."
                # Add auto-fix logic here
            elif grep -q "Type mismatch" /tmp/build_error.log; then
                echo "🔧 Fixing type errors..."
                # Add auto-fix logic here
            fi
        fi
        
        echo "⚠️ Manual intervention may be needed"
        exit 1
    fi
    
    attempt=$((attempt + 1))
done

echo "❌ Max attempts reached"
exit 1
