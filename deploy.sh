#!/bin/bash

server="root@rizinos.com"
remotePath="/mnt/data/ileies/private/software/minecraft/server/plugins"

# 3. Transfer new files to the server
echo "Uploading new files to the server..."
scp -r ./target/mikosav-2.0.0.jar ${server}:${remotePath}

# 4. SSH into the server and restart the server (uncomment to enable)
# echo "Restarting the Minecraft server..."
# ssh $server "systemctl restart mc-server"

echo "Deployment complete!"
