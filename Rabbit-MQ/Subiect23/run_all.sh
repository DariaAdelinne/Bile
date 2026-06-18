#!/bin/bash
cd "$(dirname "$0")"

# Porneste RabbitMQ daca nu ruleaza
sudo systemctl start rabbitmq-server
sleep 2

# Creaza topologia
python3 setup_rabbitmq.py
sleep 1

# Porneste toate componentele in ferestre tmux separate
tmux new-session -d -s chat -n master "python3 ChatMasterProcessor.py; read"
sleep 1
tmux new-window -t chat -n alice "python3 UserCommunicationProcessor.py alice; read"
tmux new-window -t chat -n bob "python3 UserCommunicationProcessor.py bob; read"
tmux new-window -t chat -n carol "python3 UserCommunicationProcessor.py carol; read"

# Ataseaza la sesiunea tmux
tmux attach -t chat
