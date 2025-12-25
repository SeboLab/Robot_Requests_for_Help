import socket #import socket for TCP
import sys, os, logging
import csv
import time
import logging
import random
import threading

moves = ['D1', 'D2', 'D3', 'J1', 'J2', 'J3', 'Z1', 'Z2', 'Z3']
potential_moves = ['D1', 'D2', 'D3', 'J1', 'J2', 'J3', 'Z1', 'Z2', 'Z3']


#method to keep available the moves based on difficulty 
def movesForDifficulty(difficulty):
    if difficulty == "Easy":
        return ['D1', 'D2', 'D3', 'J1', 'J2', 'J3', 'Z1', 'Z2', 'Z3']
    elif difficulty == "Medium":
        return['T1', 'T2', 'T3', 'T4','T5', 'S1', 'S2', 'S3', 'S4','S5', 'V1', 'V2', 'V3', 'V4','V5', 'W1', 'W2', 'W3', 'W4', 'W5', 'X1', 'X2', 'X3', 'X4', 'X5']
    elif difficulty == "Hard":
        return ['T1', 'T2', 'T3','T4', 'T5', 'T6', 'T7', 'S1', 'S2', 'S3','S4','S5','S6','S7', 'V1', 'V2', 'V3','V4', 'V5','V6','V7', 'W1', 'W2', 'W3', 'W4', 'W5','W6','W7', 'X1', 'X2', 'X3', 'X4', 'X5','X6','X7', 'Y1', 'Y2', 'Y3', 'Y4', 'Y5','Y6','Y7', 'Z1', 'Z2', 'Z3', 'Z4', 'Z5', 'Z6', 'Z7']
    else:
        return ['D1', 'D2', 'D3', 'J1', 'J2', 'J3', 'Z1', 'Z2', 'Z3']

def print_timestamp(message):
    logging.info(message)

#TCP Connection
class TabletSession:
    def __init__(self, host, port, condition):

        # Init vars
        self.host = host
        self.port = port
        self.correction_indices = []
        self.condition = condition

        self.is_timer_up = False
        self.timer_count = 0
        self.practice = False

        self.practice_timer_thread = None
        self.timer_thread = None
        self.stop_event = threading.Event()

        self.end = False

    def practice_timer(self):
        "Function that sets the timer on the practice"
        print_timestamp("Practice Timer started")
        if not self.stop_event.wait(60):
            print_timestamp("Practice Timer finished")
            self.is_timer_up = True

    def timer(self):
        "Function that sets timers for the game"
        print_timestamp(f"Timer {self.timer_count + 1} started")
        if not self.stop_event.wait(90):
            print_timestamp(f"Timer {self.timer_count + 1} finished")
            if self.timer_count < 4:
                self.timer_count += 1
                self.is_timer_up = True
                self.timer_thread = threading.Thread(target=self.timer, daemon=True)
                self.timer_thread.start()
            else:
                self.end = True
                self.is_timer_up = False
                print_timestamp("All four help requests completed")

    def check_resume(self):
        try:
            with open("resume.txt", "r") as f:
                resume_flag = f.read().strip()
            if resume_flag == "":
                return

            # Reset file after reading
            with open("resume.txt", "w") as f:
                f.write("")

            if resume_flag in ["0", "1", "2", "3", "4"]:
                self.stop_event.set()
                self.stop_event = threading.Event()
                self.timer_count = int(resume_flag) + 1
                self.is_timer_up = False
                print_timestamp(f"Restarting regular timer with timer_count = {self.timer_count}")
                self.timer_thread = threading.Thread(target=self.timer, daemon=True)
                self.timer_thread.start()

        except Exception as e:
            print_timestamp(f"Error reading resume.txt: {e}")

    def run(self):
            # connect to tablet

            #unclear what buffer size should be - using from laurens code
            BUFFER_SIZE = 1024
            global potential_moves
            with open("resume.txt", "w") as f:
                f.write("")

            while True:
                print_timestamp('Waiting for client connection...')
                s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
                s.setsockopt(socket.IPPROTO_TCP, socket.TCP_NODELAY, 1)

                s.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
                s.bind((self.host, self.port))
                print_timestamp(s.getsockname())
                s.listen(1)
                
                try:
                    self.conn, addr = s.accept()
                    self.conn.settimeout(None)
                    #print_timestamp(f"Connection address: {addr}")
                    msg = self.conn.recv(BUFFER_SIZE)
                    print_timestamp("Session started!")
                    returnMessage = "Return connection message \n"
                    #self.conn.sendall(b"Return connection message \n")
                    self.conn.send(returnMessage.encode())
                    #print_timestamp("sent return message")

                    #msg = String()
                    #msg.data = "Connected"
                    #self.tablet_status_pub.publish(msg)
                except KeyboardInterrupt:
                    sys.exit()

                self.is_timer_up = False

                try:
                    while True:
                        msg = self.conn.recv(BUFFER_SIZE)
                        if not msg: 
                            break

                        print_timestamp("=======================================================================")
                        msg = msg.decode().strip()
                        #print_timestamp(f"Raw received msg: {repr(msg)}")
                        
                        messages = [m.strip() for m in msg.split('\n') if m.strip()]

                        for message in messages:
                            parts = [p.strip() for p in message.split(';') if p.strip()]

                            if len(parts) >= 1:
                                msgType = parts[0]
                                msgData = parts[1] if len(parts) > 1 else ""
                                
                                #checks message type run if message is about difficulty
                                if msgType == "Difficulty":
                                    print("recived difficulty: " + msgData)
                                    #calls the method below and returns a list of possible moves depending on difficulty
                                    global moves,potential_moves
                                    moves = movesForDifficulty(msgData)
                                    #remove used moves during game without affecting original list
                                    potential_moves = moves.copy()
                                    print("Moves set to: " + str(moves))

                                # Actions based on message type
                                if (msgType == 'avatar'):
                                    if msgData == 'Red':
                                        print_timestamp("Child picks red avatar")
                                        write_to_file("Game message: Child picks red avatar")
                                        returnMessage = "Robot picks blue avatar \n"
                                        self.conn.send(returnMessage.encode())
                                        print_timestamp("Robot picks blue avatar")
                                    elif msgData == 'Blue':
                                        print_timestamp("Child picks blue avatar")
                                        write_to_file("Game message: Child picks blue avatar")
                                        returnMessage = "Robot picks red avatar \n"
                                        self.conn.send(returnMessage.encode())
                                        print_timestamp("Robot picks red avatar")
                                    threading.Thread(target=self.timer, daemon=True).start()
                                elif (msgType == "Practice"):
                                    print_timestamp("Practice Game Started")
                                    self.practice = True
                                    threading.Thread(target=self.practice_timer, daemon=True).start()


                                elif (msgType == 'Coutcome'):
                                    if msgData == 'G':
                                        print_timestamp("Child hit gold")
                                        write_to_file("Game message: Child hit gold")
                                    elif msgData == 'B':
                                        print_timestamp("Child hit bomb")
                                        write_to_file("Game message: Child hit bomb")
                                    elif msgData == '1':
                                        print_timestamp("Child is 1 away")
                                        write_to_file("Game message: Child is 1 away")
                                    elif msgData == '2':
                                        print_timestamp("Child is 2 away")
                                        write_to_file("Game message: Child is 2 away")
                                    elif msgData == '3':
                                        print_timestamp("Child is 3 away")
                                        write_to_file("Game message: Child is 3 away")
                                    elif msgData == '4':
                                        print_timestamp("Child is 4 away")
                                        write_to_file("Game message: Child is 4 away")
                                    elif msgData == '5':
                                        print_timestamp("Child is 5 away")
                                        write_to_file("Game message: Child is 5 away")          
                                    elif msgData == '6':
                                        print_timestamp("Child is 6 away")
                                        write_to_file("Game message: Child is 6 away")
                                    elif msgData == '7':
                                        print_timestamp("Child is 7 away")
                                        write_to_file("Game message: Child is 7 away")        
                                    elif msgData == '8':
                                        print_timestamp("Child is 8 away")
                                        write_to_file("Game message: Child is 8 away")                                            
                                    elif msgData == '9':
                                        print_timestamp("Child is 9 away")
                                        write_to_file("Game message: Child is 9 away")        
                                    elif msgData == '10':
                                        print_timestamp("Child is 10 away")
                                        write_to_file("Game message: Child is 10 away") 
                                    elif msgData == '11':
                                        print_timestamp("Child is 11 away")
                                        write_to_file("Game message: Child is 11 away") 
                                    elif msgData == '13':
                                        print_timestamp("Child is 13 away")
                                        write_to_file("Game message: Child is 13 away")  
                                    elif msgData == '12':
                                        print_timestamp("Child is 12 away")
                                        write_to_file("Game message: Child is 12 away")  
                                    elif msgData == '14':
                                        print_timestamp("Child is 14 away")
                                        write_to_file("Game message: Child is 14 away")  

                                                    
                                elif (msgType == 'Mistyturn'):
                                    if self.end:
                                        print_timestamp("Sending Ending Message to Tablet")
                                        finish_message = "GameFinished;GameFinished\n"
                                        print_timestamp(finish_message)
                                        self.conn.send(finish_message.encode())
                                        write_to_file("Game message: Game Finished")
                                        print_timestamp("Write to file: Game Finished")
                                        time.sleep(8)
                                    elif (self.is_timer_up):
                                        if condition in ["0", "1"]:
                                            print_timestamp("Misty "+"practice"*(self.practice) +" help request turn started")
                                            write_to_file("Game message: Start " +"practice"*(self.practice) + " help request turn")
                                            write_possible_moves(potential_moves)
                                            self.is_timer_up = False
                                            print_timestamp("Gemini move is none; waiting")
                                            next_move = read_move()
                                            time.sleep(10)
                                            if next_move not in potential_moves:
                                                write_to_file("Game message: Invalid move suggested")
                                                print_timestamp("Invalid move suggested, picking random move")
                                                next_move = pick_move()
                                                returnMessage = "Mistychoice;"+next_move + " \n"
                                            else:
                                                potential_moves.remove(next_move)
                                            returnMessage = "Mistychoice;"+next_move + " \n"
                                            print_timestamp(f"Misty chooses {next_move}")
                                            self.conn.send(returnMessage.encode())
                                            time.sleep(1)
                                        else:
                                            print_timestamp("Misty " +"practice"*(self.practice) + " take a guess turn started")
                                            write_to_file("Game message: Start " +"practice"*(self.practice) + " take a guess turn")
                                            self.is_timer_up = False
                                            time.sleep(8) # Misty is picking her next move
                                            next_move = pick_move()
                                            #write_to_file(f"Game message: Misty move is {next_move}")
                                            time.sleep(6) # Wait for misty to finish her speech
                                            returnMessage = "Mistychoice;"+next_move + " \n"
                                            print_timestamp(f"Misty chooses {next_move}")
                                            self.conn.send(returnMessage.encode())
                                            time.sleep(1)
                                    else:
                                        print_timestamp("Misty regular turn started")
                                        time.sleep(3) # Misty is picking her next move
                                        next_move = pick_move()
                                        write_to_file(f"Game message: Misty move is {next_move}")
                                        time.sleep(6) # Wait for misty to finish her speech
                                        returnMessage = "Mistychoice;"+next_move + " \n"
                                        print_timestamp(f"Misty chooses {next_move}")
                                        self.conn.send(returnMessage.encode())
                                        time.sleep(1)

                                elif (msgType == 'Moutcome'):
                                    if msgData == 'G':
                                        print_timestamp("Misty hit gold")
                                        write_to_file("Game message: Misty hit gold")
                                        refresh_moves()
                                    elif msgData == 'B':
                                        print_timestamp("Misty hit bomb")
                                        write_to_file("Game message: Misty hit bomb")
                                        refresh_moves()
                                    elif msgData == '1':
                                        print_timestamp("Misty is 1 away")
                                        write_to_file("Game message: Misty is 1 away")
                                    elif msgData == '2':
                                        print_timestamp("Misty is 2 away")
                                        write_to_file("Game message: Misty is 2 away")
                                    elif msgData == '3':
                                        print_timestamp("Misty is 3 away")
                                        write_to_file("Game message: Misty is 3 away")
                                    elif msgData == '4':
                                        print_timestamp("Misty is 4 away")
                                        write_to_file("Game message: Misty is 4 away")
                                    elif msgData == '5':
                                        print_timestamp("Misty is 5 away")
                                        write_to_file("Game message: Misty is 5 away")
                                    elif msgData == '6':
                                        print_timestamp("Misty is 6 away")
                                        write_to_file("Game message: Misty is 6 away")
                                    elif msgData == '7':
                                        print_timestamp("Misty is 7 away")
                                        write_to_file("Game message: Misty is 7 away")
                                    elif msgData == '8':
                                        print_timestamp("Misty is 8 away")
                                        write_to_file("Game message: Misty is 8 away")
                                    elif msgData == '9':
                                        print_timestamp("Misty is 9 away")
                                        write_to_file("Game message: Misty is 9 away")
                                    elif msgData == '10':
                                        print_timestamp("Misty is 10 away")
                                        write_to_file("Game message: Misty is 10 away")
                                    elif msgData == '11':
                                        print_timestamp("Misty is 11 away")
                                        write_to_file("Game message: Misty is 11 away")
                                    elif msgData == '12':
                                        print_timestamp("Misty is 12 away")
                                        write_to_file("Game message: Misty is 12 away")
                                    elif msgData == '13':
                                        print_timestamp("Misty is 13 away")
                                        write_to_file("Game message: Misty is 13 away")
                                    elif msgData == '14':
                                        print_timestamp("Misty is 14 away")
                                        write_to_file("Game message: Misty is 14 away")
                                elif (msgType == 'Homebutton'):
                                    if msgData == 'true':
                                        self.practice = False
                                        print_timestamp("Returning to home page")
                                        write_to_file("Game message: Home button pressed")
                        self.check_resume()
                                
                except KeyboardInterrupt:
                    sys.exit(0)

def tablet_message_connection(condition):
    TCP_IP = str([l for l in ([ip for ip in socket.gethostbyname_ex(socket.gethostname())[2] if not ip.startswith("127.")][:1], [[(s.connect(('8.8.8.8', 53)), s.getsockname()[0], s.close()) for s in [socket.socket(socket.AF_INET, socket.SOCK_DGRAM)]][0][1]]) if l][0][0])
    print_timestamp(TCP_IP)
    TCP_PORT = 8080

    session = TabletSession(TCP_IP, TCP_PORT, condition)
    session.run()

def read_game_flag():
    with open("game.csv", "r") as f:
        flag = f.read().strip()
    return flag

def reset_game_flag():
    with open("game.csv", 'w') as f:
        f.write("0")
    return None

def write_to_file(content):
    "The function writes game contents to data.csv and flag to flag.csv"
    "It aims to communicate with the Gemini"
    with open("data.txt", 'w') as f:
        f.write(content)
    with open("flag.csv", 'w') as f:
        f.write("1")
    return None

def write_possible_moves(content):
    with open("possible_moves.txt", 'w') as f:
        f.write(','.join(content))
    return None

def read_move():
    while True:
        with open('gemini_response.txt', 'r') as f:
            content = f.read().strip()
            if content.lower() != "none":
                move = content
                break
        time.sleep(0.1)
    with open('gemini_response.txt', 'w') as f:
        f.write("none")
    return move

def pick_move():
    global potential_moves
    move = random.choice(potential_moves)
    potential_moves.remove(move)
    return move

def refresh_moves():
    global potential_moves
    potential_moves = moves.copy()
    return None

if __name__ == "__main__":
    reset_game_flag()

    partipant_code = input("Enter participant code: \n")
    log_filename = f"TCP_{partipant_code}.txt"
    logging.basicConfig(
        level=logging.INFO,
        format='[%(asctime)s] %(message)s',
        datefmt='%Y-%m-%d %H:%M:%S',
        handlers=[
            logging.FileHandler(log_filename),
            logging.StreamHandler()
        ]
    )

    condition = input("Enter condition 0, 1, 2, or 3: \n")
    print_timestamp(f"Participant code: {partipant_code}")
    print_timestamp(f"Condition: {condition}")
    time.sleep(1)
    
    tablet_message_connection(condition)