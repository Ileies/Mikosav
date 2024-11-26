# deploy.ps1

$privateKeyPath = "C:\Users\elias.klassen\ssh_keys\win_pinix_root.ppk"
$server = "root@rizinos.com"
$remotePath = "/mnt/data/ileies/private/software/minecraft/server/plugins"

# 3. Transfer new files to the server
Write-Host "Uploading new files to the server..."
scp -i $privateKeyPath -r ./target/mikosav-2.0.0.jar ${server}:$remotePath
#hidden files are not copied automatically using scp

# 4. SSH into the server and restart the server
Write-Host "Restarting the Minecraft server..."
$pm2Commands = "systemctl restart mc-server"
#pm2 restart pm2.config.cjs --update-env
ssh -i $privateKeyPath $server $pm2Commands

Write-Host "Deployment complete!"
