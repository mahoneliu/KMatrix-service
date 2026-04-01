@echo off
git add .
git commit -F commit_msg.txt
git push
del commit_msg.txt
del log_service.txt
del %0
