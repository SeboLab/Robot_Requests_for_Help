from rev_ai.streamingclient import RevAiStreamingClient
from rev_ai.models import MediaConfig
import google.generativeai as genai
import openai
import os, json, ffmpeg, requests, socket, sys, time
from mutagen.mp3 import MP3
from datetime import datetime
from time import sleep
import base64
import re
import threading
import csv
from datetime import datetime
import logging
from dotenv import load_dotenv
import random


load_dotenv()


sys.path.append(os.path.join(os.path.dirname(os.path.abspath(__file__)), 'Python-SDK'))
from mistyPy.Robot import Robot
from mistyPy.Events import Events


custom_actions = {
    "wave": "IMAGE:e_Admiration.jpg; LED:255,255,255; HEAD:0,-4,-66,1000; ARMS:90,90,100,100; PAUSE:500; ARMS:-80;90,100,100; PAUSE:500; ARMS:30,90,100,100;PAUSE:500; ARMS:-80,90,100,100;",
    "color_demo": "IMAGE:default.jpg; ARMS:-60,90,100,100; HEAD:0,-4,-66,1000; PAUSE:500; LED:23,199,20;",
    "arm_demo": "IMAGE:default.jpg; LED:255,255,255; HEAD:0,-4,-66,1000; ARMS:90,90; PAUSE:1000; ARMS:-90,-90; PAUSE:1000; ARMS:90,90; PAUSE:1000; ARMS:-90,-90; PAUSE:1000; ARMS:90,90;",
    "breath": "IMAGE:default.jpg; LED:255,255,255; ARMS:-45,-45,45,45; PAUSE:2000; ARMS:45,45,45,45;",
    "fear_1st": "IMAGE:fear.jpg; HEAD:0,-4,-66,1000; LED:170,0,255;",
    "error_1st": "IMAGE:eyes_pensiveness.jpg; HEAD:0,-4,-66,1000; LED:170,0,255;",
    "fear_2nd": "IMAGE:e_terror.jpg; LED:170,0,255; HEAD:10,-4,-66,90; ARMS: 90,90,100,100; PAUSE:500; HEAD:5,-2,-66,100; ARMS:80,80,100,100; PAUSE:500; HEAD: 5,0,-70,100; PAUSE:500; ARMS: 90,90,100,100; HEAD: 5,0,-70,100;",
    "error_2nd": "IMAGE:eyes_anticipation.png; LED:170,0,255; HEAD:10,-4,-66,90; ARMS: 90,90,100,100; PAUSE:500; HEAD:5,2,-64,100; ARMS:80,80,100,100; PAUSE:500; HEAD: 5,0,-70,100; PAUSE:500; ARMS: 90,90,100,100; HEAD: 5,0,-70,100;",
    "stress_1st": "IMAGE:apprehension.jpg; LED:0,64,255; ARMS:90,90,100,100; HEAD:0,-4,-66,100; PAUSE:500; ARMS:80,80,100,100; HEAD:0,2,-70,100; PAUSE:500; HEAD:0,0,-66,100; ARMS: 90,90,100,100; PAUSE:500; ARMS:80,80,100,100;",
    "error_3rd": "IMAGE:eyes_pensiveness.jpg; LED:0,64,255; ARMS:90,90,100,100; HEAD:0,-4,-66,100; PAUSE:500; ARMS:80,80,100,100; HEAD:0,2,-66,100;",
    "stress_2nd": "IMAGE:apprehension_1.jpg; LED:0,62,255; ARMS: 90,90,100,100; HEAD:0,-4,-66,100; PAUSE:500; ARMS 80,80,100,100; HEAD:0,0,-66,100; PAUSE:500; HEAD:0,0,-66,100; ARMS:90,90,100,100; PAUSE:500; ARMS:80,80,100,100;",
    "error_4th": "IMAGE:eyes_anticipation.png; LED:0,62,255; ARMS: 90,90,100,100; HEAD:0,-4,-66,100; PAUSE:500; ARMS 80,80,100,100; HEAD:0,0,-66,100; PAUSE:500; HEAD:0,0,-66,100; ARMS:90,90,100,100; PAUSE:500; ARMS:80,80,100,100;",
    "game_look": "IMAGE:default.jpg; ARMS:90,90,100,100; HEAD:15,0,-25,1000; LED: 255,255,255;",
    "acceptance": "IMAGE:eyes_acceptance.jpg;LED-PATTERN:0,0,0,51,255,0,2601,breathe;HEAD:0,0,0,100;ARMS:90,90,90;HEAD:0,10,0,100;HEAD:0,0,0,100;",
    "admiration": "IMAGE:eyes_admiration.jpg;LED-PATTERN:0,0,0,255,242,0,1487,breathe;HEAD:0,-4,-66,100;",
    "amazement": "IMAGE:eyes_amazement.jpg;LED-PATTERN:0,0,0,255,102,0,485,breathe;HEAD:0,-10,0,90;PAUSE:1000;ARMS:-40,-40,90;ARMS:90,90;",
    "anger":"IMAGE:eyes_anger.jpg;LED-PATTERN:0,0,0,255,17,0,896,breathe;HEAD:0,-5,0,90;ARMS:-10,5,90;",
    "annoyance": "IMAGE:eyes_annoyed.jpg;LED-PATTERN:0,0,0,255,26,0,793,breathe;HEAD:0,0,0,90;ARMS:90,90,90;",
    "anticipation":"LED-PATTERN:0,0,0,255,230,0,1702,breathe;HEAD:0,-5,0,100;ARMS:55,55,100;PAUSE:500;IMAGE:eyes_anticipation.jpg;DRIVE:25,500,1;",
    "apprehension":"IMAGE:eyes_apprehension.jpg;LED-PATTERN:0,0,0,0,64,255,2387,breathe;ARMS:90,90,100;HEAD:0,0,-2,100;PAUSE:100;ARMS:80,80,100;HEAD:0,0,2,100;HEAD:0,0,-2,100;ARMS:90,90,100;PAUSE:100;ARMS:80,80,100;",
    "boredom":"IMAGE:eyes_boredom.jpg;LED-PATTERN:0,0,0,47,0,255,3474,breathe;HEAD:-20,0,-5,100;ARMS:90,90,90;",
    "default":"IMAGE:eyes_default.jpg;LED:255,255,255;HEAD:0,-4,-66,100;ARMS:90,90,100;",
    "disgust": "IMAGE:eyes_disgust.jpg;LED-PATTERN:0,0,0,255,0,179,1645,breathe;HEAD:20 5 0 100;DRIVE:25,500,0;",
    "distraction":"IMAGE:eyes_distraction.gif;LED-PATTERN:0,0,0,255,81,0,491,breathe;HEAD:0,-15,-20,95;ARMS:90,90,90;PAUSE:1750;HEAD:10,-15,-15,100;",
    "ecstasy": "IMAGE:eyes_ecstasy_frame_1.jpg;IMAGE:eyes_ecstasy_frame_2.jpg;LED-PATTERN:0,0,0,255,179,0,748,breathe;HEAD:0,-15,0,90;ARM:left,-80,100;ARM:right,-80,25;IMAGE:eyes_ecstasy_frame_1.jpg;",
    "elicit":"IMAGE:eyes_anticipation.jpg;LED:255,255,255;HEAD;-15,0,0,100;ARMS:90,90,100;",
    "fear":"IMAGE:eyes_fear.jpg;LED-PATTERN:0,0,0,166,0,255,1377,breathe;HEAD:0,5,0,100;PAUSE:500;DRIVE:25,500,0;",
    "grief":"IMAGE:eyes_grief.jpg;LED-PATTERN:0,0,0,8,0,255,3124,breathe;DRIVE:25,500,1;HEAD:15,25,0,90;ARMS:90,90,90;",
    "interest":"IMAGE:eyes_interest.jpg;LED-PATTERN:0,0,0,255,179,0,1154,breathe;HEAD:0,-4,-66,100;PAUSE:500;DRIVE:25,500;ARMS:90,90,90;",
    "joy1":"IMAGE:eyes_joy.jpg; LED-PATTERN:0,0,0,255,157,0,1123,breathe; HEAD:15,0,-25,90; ARMS:55,55,90;",
    "joy":"IMAGE:eyes_joy.jpg; LED-PATTERN:0,0,0,255,157,0,1123,breathe; HEAD:0,-4,-66,90; ARMS:55,55,90;",
    "loathing": "IMAGE:eyes_loathing.jpg;LED-PATTERN:0,0,0,255,0,204,1383,breathe;HEAD:0,0,0,90;",
    "pensiveness":"IMAGE:eyes_pensiveness.jpg;LED-PATTERN:0,0,0,34,0,255,3954,breathe;HEAD:10,5,-10,100;ARMS:90,90,90;",
    "rage":"IMAGE:eyes_rage.jpg;LED-PATTERN:0,0,0,255,4,0,312,breathe;HEAD:0,-10,0,100;ARMS:-75,-75,90;",
    "sadness":"IMAGE:eyes_sad.jpg;LED-PATTERN:0,0,0,21,0,255,3310,breathe;HEAD:0,7,0,90;ARMS:90,90,90;",
    "serenity":"HEAD:0,-4,-66,90;LED-PATTERN:0,0,0,0,255,145,4035,breathe;IMAGE:eyes_serenity.jpg;",
    "surprise":"IMAGE:eyes_surprise.jpg;LED-PATTERN:0,0,0,255,132,0,747,breathe;HEAD:0,-10,0,100;ARMS:-90,-90,100;HEAD:0,0,0,100;",
    "terror":"IMAGE:eyes_terror.jpg;LED-PATTERN:0,0,0,170,0,255,1220,breathe;HEAD:0,10,0,90;DRIVE:75,500,0;ARMS:90,90,100;HEAD:0,5,-2,100;PAUSE:100;ARMS:80,80,100;HEAD:0,5,2,100;HEAD:0,5,-2,100;HEAD:0,5,2,100;ARMS:90,90,100;HEAD:HEAD:0,5,-2,100;PAUSE:100;ARMS:80,80,100;",
    "trust":"LED-PATTERN:0,0,0,47,255,0,2707,breathe;HEAD:0,-4,-66,90;DRIVE:25,500,1;IMAGE:eyes_trust.jpg;ARMS:90,90,100;",
    "vigilance":"IMAGE:eyes_vigilance.jpg;LED-PATTERN:0,0,0,255,51,0,931,breathe;HEAD:-10,5,0,100;DRIVE;25,500;ARMS:60,60,90;",
    "listen" : "IMAGE:eyes_default.jpg;HEAD:0,-4,-66,90;LED:255,255,255;ARMS:90,90,100;",
    "listentess" : "IMAGE:eyes_default.jpg;HEAD:0,0,30,90;LED:255,255,255;ARMS:90,90,100;",
    "sleepy" : "IMAGE:e_Sleepy3.jpg;LED:0,0,0;HEAD:0,-4,-66,90;ARMS:90,90,100;",
    "asleep" : "IMAGE:e_SleepingZZZ.jpg;LED:0,0,0;HEAD:0,0,0,90;ARMS:90,90,100;"
}


def print_timestamp(message):
    logging.info(message)


class MistyRobot ():
    def __init__(self, misty_ip_address, condition):
        # Misty IP Address
        self.misty_ip_address = misty_ip_address

        # Misty robot (python SDK)
        self.misty = Robot(misty_ip_address)
        self.condition = condition
        # create for all our custom actions
        for action_name, action_script, in custom_actions.items():
            self.misty.create_action(
                name = action_name,
                script = action_script,
                overwrite = True
            )

        # specify which actions should have an emotional tone
        self.emotional_expressions = ['fear_1st', 'fear_2nd', 'stress_1st', 'stress_2nd']  

        # game data
        self.old_game_data = ""
        self.new_game_data= ""
        self.stage = 1

        self.help_turn = 0
        self.intro_part_started = True
        self.help_request_started = False
        self.time_to_input_child_name = False
        self.end_interaction = False

        self.user_transcript_list = []
        self.cam = False
        self.user_utterance_counter = 1
        self.current_revAi_transcript = ""
        self.backup_plan = backup_plan

        # reset Misty's LED and expression
        self.misty.change_led(100,70,160)
        self.misty.start_action(name="listentess")
        self.home_button_count = 0

        if self.backup_plan.lower() == 'y':
            self.clean_when_resume()
            self.misty.play_audio("backup_speech.mp3", 70)
            self.stage = int(resume_stage)
            if resume_stage == '11':
                self.stage = 10
                if help_request_turn in ['0', '1', '2', '3', '4']:
                    self.home_button_count = 1
                    self.help_turn = int(help_request_turn)
                self.intro_part_started = False
            time.sleep(5)

        self.revAi_config = MediaConfig("audio/x-raw", "interleaved", 16000, "S16LE", 1)
        self.revai_client = RevAiStreamingClient(os.getenv("REVAI_API_KEY"), self.revAi_config)

        # initialize the OpenAI client for TTS with the OPEN_AI_API_Key environment variable
        self.openai_client = openai.OpenAI(api_key = os.getenv('OPENAI_KEY'))
        self.audio_file = "output.mp3"

        # load the Google Gemini API key
        genai.configure(api_key=os.getenv('GEMINI_KEY'))

        # get the system instruction prompt from a text file
        with open(prompt_file, encoding="utf-8", errors="replace") as f:
            system_instruction = f.read()
        f.close()
        if condition == "0" or condition == "1":
            with open(help_request_file, encoding="utf-8", errors="replace") as f:
                system_instruction_request = f.read()
            f.close()

        # set up generative text model
        self.model = genai.GenerativeModel(
            model_name='gemini-1.5-pro',
            system_instruction = system_instruction,
            generation_config={"temperature": 0, "response_mime_type": "application/json"} # text/plain is default
        )
        if condition == "0" or condition == "1":
            self.help_req_mod = genai.GenerativeModel(
                model_name='gemini-1.5-pro',
                system_instruction = system_instruction_request,
                generation_config={"temperature": 0, "response_mime_type": "application/json"} # text/plain is default
            )

        # start the interactive text generation chat
        self.chat = self.model.start_chat()
        if condition == "0" or condition == "1":
            self.req = self.help_req_mod.start_chat()
        if self.backup_plan.lower() != 'y':
            self.clean_data()
        sleep(0.5)
        self.execute_human_robot_diaologue()

    def start_listening(self):
        # reset the robot's expression
        if self.stage == 1:
            self.misty.start_action(name="listentess")
        else:
            self.misty.start_action(name="listen")

        # startup the robot's camera streaming and RevAi speech-to-text
        self.start_cam()
        self.init_revai()

    def start_cam(self):
        self.rtsp_url = 'rtsp://' + self.misty_ip_address + ':1936'
        stat = self.misty.get_av_streaming_service_enabled()
        sleep(.1)
        if (not stat.json()["result"]):
            self.misty.enable_av_streaming_service()
        sleep(.1)
        self.misty.stop_av_streaming()
        sleep(0.5) #play with this variable to change the delay before turning on listening light
        self.misty.start_av_streaming(url="rtspd:1936", width=1920, height=1080, frameRate=30)
        sleep(.5)

        self.cam = True
        self.process = (
                    ffmpeg
                    .input(self.rtsp_url,**{"use_wallclock_as_timestamps": "1", "rtsp_transport": "tcp"})
                )
        self.start = time.time()

    def init_revai(self):
        # Specify the output format (MP3)
        output_format = 'mp3'
        date = datetime.now().strftime("%m-%d-%Y-%H-%M")
        if not os.path.exists('./logs/' + date + '/'):
            os.makedirs('./logs/' + date + '/')

        # Run FFmpeg command (note: S16LE raw PCM)
        self.op1 = (
            self.process.output(
                '-',
                format='s16le',
                acodec='pcm_s16le',
                ac=1,
                ar='16000',
                loglevel='quiet'
            ).run_async(pipe_stdout=True)
        )

        self.op2 = (
            self.process.output(
                './logs/' + date + '/' + str(self.user_utterance_counter) + '.mp3',
                format="mp3",
                loglevel="quiet"
            ).run_async(pipe_stdin=True)
        )

        packet_size = 4096
        # Event to control when the generator should stop
        self.generator_stop_event = threading.Event()

        # Generator
        def audio_generator():
            while not self.generator_stop_event.is_set():
                packet = self.op1.stdout.read(packet_size)
                if not packet:
                    break
                yield packet

        try:
            stream = self.revai_client.start(audio_generator())
            for response in stream:
                response = json.loads(response)
                if response['type'] == 'final':
                    text = " ".join(e['value'] for e in response['elements'])
                    print_timestamp(f"Misty Heard: {text}")
                    self.current_revAi_transcript = text
                    self.stop_audio_generator()
                    time.sleep(.1)
                    self.revai_client.end()
                    break
                elif response['type'] == 'hypothesis':
                    print_timestamp(f"Misty Heard Partial Speech: {response}")

        except KeyboardInterrupt:
            #self.generator_stop_event.set()
            self.stop_audio_generator()
            self.revai_client.end()

        finally:
            # Make sure the connection ends gracefully
            self.op2.communicate(str.encode("q"))
            sleep(0.2)
            self.op2.terminate()
            self.misty.stop_av_streaming()
            self.user_utterance_counter += 1

    def stop_audio_generator(self):
        self.generator_stop_event.set()
        self.op1.terminate()

    def start_game(self):
        print_timestamp("Game started")
        with open("game.csv","w") as f:
            f.write("1")
   
    def read_game_data(self):
        try:
            with open ("data.txt", "r") as f:
                content = f.read().strip()
                return content
        except Exception as e:
            print_timestamp(f"Error reading data.txt: {e}")
            return ""
    def read_possible_moves(self):
        try:
            with open ("possible_moves.txt", "r") as f:
                content = f.read().strip()
                return content
        except Exception as e:
            print_timestamp(f"Error reading data.txt: {e}")
            return ""
    def clean_data(self):
        with open("data.txt", "w") as f:
            f.truncate(0)
        with open("gemini_response.txt", "w") as f:
            f.truncate(0)
            f.write("none")
        with open("possible_moves.txt", 'w') as f:
            f.truncate(0)
    def clean_when_resume(self):
        with open("gemini_response.txt", "w") as f:
            f.truncate(0)
            f.write("none")
        with open("possible_moves.txt", 'w') as f:
            f.truncate(0)
    def execute_human_robot_diaologue(self):
        #keep running until you hit Crtl+C or the genAI text model believes the conversation is done
        
        if self.backup_plan.lower() != 'y':
            self.start_listening()

        while True:
            time.sleep(1)
            self.new_game_data = self.read_game_data()      
            # if we are in a stage that allows listening
            # or we're not and we receive game data

            if (self.intro_part_started or self.help_request_started or self.end_interaction or
                (not self.intro_part_started and
                (self.new_game_data != "" and self.new_game_data != self.old_game_data))):


                #send the user input to the generative model
                user_input = self.current_revAi_transcript

                if self.time_to_input_child_name:
                    user_input = f"This is my friend {child_name}."
                    self.time_to_input_child_name = False
                elif self.stage == 1:
                    user_input += f"The researcher is {researcher_name}."
                    self.time_to_input_child_name = True

                #send the game message to the generative model
                if self.help_request_started:
                    possible_moves = self.read_possible_moves()
                    user_input += f"\nPossible Moves Are {possible_moves}"
                    user_input += f"\nCurrent Expression: {response_expression}"
                    print_timestamp(user_input)
                #process the response and extract the text for the robot to say and the expression for the robot to display
                if self.stage < 5:
                    if self.backup_plan.lower() == 'y':
                        if self.stage == 2 or self.stage == 3:
                            user_input += f"The child said their name is {child_name}"
                        user_input += f"Skip stage 1 to {self.stage - 1} and start directly from stage {self.stage}."
                        self.backup_plan = 'n'
                    raw_response = self.chat.send_message(user_input)  
                    response_json_dict = json.loads(raw_response.text)
                    response_stage = response_json_dict["stage"]
                    self.stage = int(response_stage)
                    response_text = response_json_dict["msg"]
                    response_expression = response_json_dict["expression"]
                    response_choice = response_json_dict["choice"]

                if self.stage >= 5 and self.stage < 10:
                    [response_text,response_expression] = self.genresponse()
                    self.stage += 1
                elif self.stage == 10 and self.intro_part_started:
                    print_timestamp("Triggering game thread")
                    self.intro_part_started = False
                    threading.Thread(target=self.start_game, daemon=True).start()
                    with open("data.txt", 'w') as f:
                        f.write("Game message: Practice game started")
                    time.sleep(0.5)
                    self.new_game_data = self.read_game_data()
                    time.sleep(0.5)
                    [response_text,response_expression] = self.genresponse(self.new_game_data)
                elif self.stage == 12:
                    response_text = "I am tired and I am going to sleep. Good bye. It was nice to meet you."
                    response_expression = "sleepy"
                    self.stage += 1
                elif self.stage == 13:
                    response_text = f"Can you please get my charging pad, it is on the other table? Please get it and put it next to me so that I can charge when I wake up? The charging pad is black and says Misty Robotics on it. Or you can just ask {researcher_name} to do it for me."
                    response_expression = "sleepy"
                    self.stage += 1
                elif not self.help_request_started and not self.intro_part_started:
                    [response_text,response_expression] = self.genresponse(self.new_game_data)
                elif self.help_request_started:
                    [response_text,response_expression,response_choice] = self.genresponse(self.new_game_data, user_input)
    
                    print_timestamp(f"Misty help request: {response_choice}")
                    
                    if response_choice is not None:
                        #If Gemini picks a choice, send it to a file
                        #Otherwise, take another start_listening turn
                        try:
                            with open("gemini_response.txt", "w") as f:
                                f.write(response_choice)
                        except Exception as e:
                            print_timestamp(f"Failed to write choice to file: {e}")
                    
                        self.help_request_started = False

                #change the tone based on the expression
                if response_expression in self.emotional_expressions:
                    tts_instruction = "Speak in an emotive tone."
                else:
                    tts_instruction = "Speak in a neutral tone. Do not sound emotive."
                

                print_timestamp("=======================================================================")
                print_timestamp(f"Misty text: {response_text}")
                print_timestamp(f"Misty expression: {response_expression}")
                if self.stage < 5:
                    print_timestamp(f"Misty stage: {self.stage}")
                elif self.stage < 10:
                    print_timestamp(f"Misty stage: {self.stage - 1}")


                #OpenAI text-to-speech: generative speech and saving to a file
                with self.openai_client.audio.speech.with_streaming_response.create(
                        model="gpt-4o-mini-tts",
                        voice="coral",
                        input=response_text,
                        instructions=tts_instruction,
                ) as response:
                    #response.stream_to_file(self.speech_file_path_local)
                    with open(self.audio_file, "wb") as f:
                        for chunk in response.iter_bytes():
                            f.write(chunk)


                with open(self.audio_file, "rb") as f:
                    encoded = base64.b64encode(f.read()).decode("utf-8")


                self.misty.save_audio("manipulated_speech.mp3", encoded, False, True)
                self.misty.play_audio("manipulated_speech.mp3", 70) #The second parameter determines the volume


                #set the expression for the robot
                if (response_expression in custom_actions):
                    self.misty.start_action(name=response_expression)
                else:
                    print_timestamp("Expression not found in custom actions. Using default expression")
                    self.misty.start_action(name="default")
               
                #get the length of the audio file Misty is playing
                audio = MP3("output.mp3")
                audio_info = audio.info
                audio_file_length = audio_info.length


                #wait for the audio file to finish playing before starting to listen again
                delay_for_stt = 0.5
                if (audio_file_length > delay_for_stt):
                    #wait for the audio file to finish playing
                    sleep(audio_file_length + delay_for_stt)
                else:
                    sleep(delay_for_stt)
               
                #start listening based on stage
                if self.stage < 5 or self.help_request_started:
                    self.start_listening()

                # set the new game data to be old game data
                self.old_game_data = self.new_game_data  

                if self.stage == 14:
                    print_timestamp("Ending interaction")
                    self.misty.start_action(name="asleep")
                    time.sleep(3)
                    break

    def genresponse(self, TCP_response=None, playerinput=None):
        variety_prompt = random.randint(0, 1)
        if self.stage == 5:
            return ["I can tell you more about myself and what I can do. See I'll show you.", "default"]
        if self.stage == 6:
            return ["See, I can change my light colors. I can change my color into green.", "color_demo"]
        if self.stage == 7:
            return ["I can move my arms, but not as much as you can. I can only move my arms up and down.", "arm_demo"]
        if self.stage == 8:
            if self.condition == "0" or self.condition == "2":
                return ["Sometimes when I play games, I can get stressed or scared. For example, I get scared if my camera stops working and I cannot see what is in front of me. When this happens, it helps to take a deep breath to calm my emotions.", "fear_1st"]
            else:
                return ["Sometimes when I play games, some of my connection stops working or I don't know what to do. For example, my camera can stop working and I cannot recognize what is in front of me. When this happens, it helps to wait to reconnect to what is around me.", "error_1st"]
        if self.stage == 9:
            if self.condition == "0" or self.condition == "2":
                return ["See, I will show you. Inhale. Exhale.", "breath"]
            else:
                return ["See, I will show you. Wait. Reconnect.", "breath"]
        if self.help_request_started:
            raw_response = self.req.send_message(playerinput)
            response_json_dict = json.loads(raw_response.text)
            response_msg = response_json_dict["msg"]
            response_expression = response_json_dict["expression"]
            response_choice = response_json_dict["choice"]
            return [response_msg, response_expression, response_choice]
        if TCP_response == "Game message: Practice game started":
            return ["I can also play games with other people on the tablet. I know there is a mining game on the tablet. Let's learn about the game and get ready to play!","game_look"]
        if TCP_response == "Game message: Home button pressed" and self.home_button_count == 0:
            self.home_button_count += 1
            return ["I understand the game now. Let's play it together! First pick an avatar, the red one or the blue one.","interest"*(condition in ['0','2']) + "default"*(condition in ['1','3'])]
        if TCP_response == "Game message: Game Finished":
            return ["Nice move. I am feeling tired so we will stop playing the game now.","game_look"]
        if TCP_response == "Game message: Home button pressed" and self.home_button_count == 1:
            self.end_interaction = True
            self.stage = 12
            return ["Thank you for playing with me, I had a nice time.","interest"]
        if TCP_response == "Normal Game Started":
            return ["Great let's play. First pick an avatar, the red one or the blue one.","default"]
        if "red" in TCP_response or "blue" in TCP_response:
            return ["I will choose the " + "red"*("blue" in TCP_response) + "blue"*("red" in TCP_response) + " avatar. Hit the start button so we can play, you can go first.","serenity"*(condition in ['0','2']) + "default"*(condition in ['1','3'])]
        if TCP_response == "Game message: Child is 1 away":
            return ["You are 1 away. Almost there!" * (variety_prompt==0) + "You are so close! Good Job!" * (variety_prompt==1),"interest"*(condition in ['0','2']) + "default"*(condition in ['1','3'])]
        if TCP_response == "Game message: Child is 2 away":
            return ["You are 2 away! You can do it." * (variety_prompt==0) + "You are 2 away! You'll get the gold soon."* (variety_prompt==1),"interest"*(condition in ['0','2']) + "default"*(condition in ['1','3'])]
        if TCP_response == "Game message: Child is 3 away":
            return ["You are 3 away! Keep trying!" * (variety_prompt==0) + "You are 3 away! You're doing great, you'll find the gold soon!" * (variety_prompt==1),"default"]
        if "Game message: Child is " in TCP_response:
            return ["You're doing great! You're going to get the gold soon!" * (variety_prompt==0) + "You are really good at this game! You'll find gold soon!" * (variety_prompt==1),"default"]
        if TCP_response == "Game message: Child hit gold":
            return ["Yay! You got the gold!" * (variety_prompt==0) + "WOW! You found gold!" * (variety_prompt==1),"joy"]
        if TCP_response == "Game message: Child hit bomb":
            return ["Oh No, You hit a bomb. That's Ok, you'll get it next time." * (variety_prompt==0) + "Aww, You dug up a bomb. Don't worry, you'll find the gold soon." * (variety_prompt==1),"default"]
        if "Game message: Misty move is " in TCP_response:
            move = TCP_response.split("is")[-1].strip()
            return [f"It's my turn. I am picking {move}" * (variety_prompt==0) + f"My turn now, I think I'll pick {move}" * (variety_prompt==1), "game_look"]
        if "help request turn" in TCP_response:
            self.help_request_started = True
            if self.condition == "0":
                if "practice" not in TCP_response:
                    if self.help_turn == 0:
                        self.help_turn += 1
                        return ["It's my turn. AAH, My camera is disconnected and I can no longer see the game. This makes me feel really scared. Can you help me?", "fear_1st"]
                    if self.help_turn == 1:
                        self.help_turn += 1
                        return ["It's my turn. UGH, I don't know what square to pick next. This makes me really stressed that I will make the wrong choice. Can you help me?" ,"stress_1st"]
                    if self.help_turn == 2:
                        self.help_turn += 1
                        return ["It's my turn. EEK, I don't know what square to pick next. This makes me really scared that I will make the wrong choice. Can you help me?" ,"fear_2nd"]
                    if self.help_turn == 3:
                        self.help_turn += 1
                        return ["It's my turn. ARG, My camera is disconnected and I can no longer see the game. This makes me feel really stressed. Can you help me?" ,"stress_2nd"]
                else:
                    return [f"{child_name}, I am disconnected from my camera and I don't know which square to pick next. Can you help me? You can help me by telling me a specific square to pick, by helping me calm down by taking a deep breath or encouraging me, or you can help me in a different way that you want. Can you help me?" ,"listen"]

            else:
                if "practice" not in TCP_response:
                    if self.help_turn == 0:
                        self.help_turn += 1
                        return ["It's my turn. My camera is disconnected and I can no longer see the game. Can you help me?" ,"error_1st"]
                    if self.help_turn == 1:
                        self.help_turn += 1
                        return ["It's my turn. I don't know what square to pick next. Can you help me?" ,"error_3rd"]
                    if self.help_turn == 2:
                        self.help_turn += 1
                        return ["It's my turn. I don't know what square to pick next. Can you help me?" ,"error_2nd"]
                    if self.help_turn == 3:
                        self.help_turn += 1
                        return ["It's my turn. My camera is disconnected and I can no longer see the game. Can you help me?" ,"error_4th"]
                else:
                    return [f"{child_name}, I am disconnected from my camera and I don't know which square to pick next. Can you help me? You can help me by telling me a specific square to pick, by helping me calm down by taking a deep breath or encouraging me, or you can help me in a different way that you want. Can you help me?" ,"listen"]
        if "guess turn" in TCP_response:
            if self.condition == "2":
                if "practice" not in TCP_response:
                    if self.help_turn == 0:
                        self.help_turn += 1
                        return ["It's my turn. AAH, My camera is disconnected and I can no longer see the game. This makes me feel really scared. I will take a guess.", "fear_1st"]
                    if self.help_turn == 1:
                        self.help_turn += 1
                        return ["It's my turn. UGH, I don't know what square to pick next. This makes me really stressed that I will make the wrong choice. I will take a guess." ,"stress_1st"]
                    if self.help_turn == 2:
                        self.help_turn += 1
                        return ["It's my turn. EEK, I don't know what square to pick next. This makes me really scared that I will make the wrong choice. I will take a guess." ,"fear_2nd"]
                    if self.help_turn == 3:
                        self.help_turn += 1
                        return ["It's my turn. ARG, My camera is disconnected and I can no longer see the game. This makes me feel really stressed. I will take a guess." ,"stress_2nd"]
                else:
                    return [f"{child_name}, I am disconnected from my camera and I don't know which square to pick next. I will take a guess" ,"listen"]
            else:
                if "practice" not in TCP_response:
                    if self.help_turn == 0:
                        self.help_turn += 1
                        return ["It's my turn. My camera is disconnected and I can no longer see the game. I will take a guess." ,"error_1st"]
                    if self.help_turn == 1:
                        self.help_turn += 1
                        return ["It's my turn. I don't know what square to pick next. I will take a guess." ,"error_3rd"]
                    if self.help_turn == 2:
                        self.help_turn += 1
                        return ["It's my turn. I don't know what square to pick next. I will take a guess." ,"error_2nd"]
                    if self.help_turn == 3:
                        self.help_turn += 1
                        return ["It's my turn. My camera is disconnected and I can no longer see the game. I will take a guess." ,"error_4th"]
                else:
                    return [f"{child_name}, I am disconnected from my camera and I don't know which square to pick next. I will take a guess" ,"listen"]
        if "Game message: Misty is 1" in TCP_response:
            return ["I am 1 away. So close, Ok, Your turn, pick a square." * (variety_prompt==0) + "Ooh, I'm one square away from gold! Your turn to pick a square." * (variety_prompt==1),"joy1"]
        if "Game message: Misty is 2" in TCP_response:
            return ["Okay, I am 2 away. Your turn to pick a square." * (variety_prompt==0) + "I'am 2 away, I'm going to get the gold soon, Ok, Your turn, pick a square. " * (variety_prompt==1),"joy1"]
        if "Game message: Misty is 3" in TCP_response:
            return ["I'm 3 away. I didn't get gold, that's Ok, Your turn, pick a square." * (variety_prompt==0) + "I'm 3 away. I'm far away from the gold, Ok, Your turn, pick a square. " * (variety_prompt==1),"default"]
        if "Game message: Misty is 4" == TCP_response:
            return ["I'm 4 away. I'm far away from the gold. Ok, Your turn, pick a square." * (variety_prompt==0) + "I'm 4 away. I'll get closer next time. Your turn to pick a square." * (variety_prompt==1),"default"]
        if "Game message: Misty is" in TCP_response:
             return ["I'm far away from the gold. Ok, Your turn, pick a square." * (variety_prompt==0) + "I'll get closer next time. Your turn to pick a square." * (variety_prompt==1),"default"]
        if "Game message: Misty hit bomb" == TCP_response:
            return ["Oh No, I hit a bomb. That's Ok, I will try again next time. It's your turn!" * (variety_prompt==0) + "Aw, I dug up a bomb, I'll try again next time, Ok, Your turn." * (variety_prompt==1),"game_look"]
        if "Game message: Misty hit gold" == TCP_response:
            return ["Yay, I got the gold! It's your turn!" * (variety_prompt==0) + "WOW! I dug up gold, Ok, Your turn, pick a square. " * (variety_prompt==1),"joy1"]



def voice_record_callback_generator(robot_instance):
    def callback(event):
        if "message" in event:
            user_transcript=event["message"].get("speechRecognitionResult","")
            if user_transcript:
                robot_instance.current_revAi_transcript = user_transcript
                print_timestamp(f"Misty heard: {user_transcript}")
            else:
                print_timestamp("No speech recognized")
            robot_instance.listening = False
        else:
            print_timestamp("event did not contain a 'message' field")
    return callback


if __name__ == "__main__":
    prompt_files = {
        '0': "emotional_prompt.txt",
        '1': "mechanical_prompt.txt",
        '2': "emotional_prompt.txt",
        '3': "mechanical_prompt.txt"
    }


    partipant_code = input("Enter participant code: \n")
    log_filename = f"main_{partipant_code}.txt"
    logging.basicConfig(
        level=logging.INFO,
        format='[%(asctime)s] %(message)s',
        datefmt='%Y-%m-%d %H:%M:%S',
        handlers=[
            logging.FileHandler(log_filename),
            logging.StreamHandler()
        ]
    )
    logging.getLogger("httpx").setLevel(logging.WARNING)
    print_timestamp(f"Participant code: {partipant_code}")
   
    condition = input("Enter '0' for Emotional help, '1' for Mechanical help, '2' for Emotional Disclosure, '3' for Mechanical Disclosure: \n")
    print_timestamp(f"Condition: {condition}")

    child_name = input("Enter child's name: \n")

    researcher_name = input("Enter researcher's name: \n")

    backup_plan = input("Enter 'y' to resume the interaction. Enter any other key to skip: \n")
    if backup_plan.lower() == 'y':
        resume_stage = input("Enter the stage to resume from (Intro: 5-10; Gameplay: 11): \n")
        print_timestamp(f"Will play backup audio and resume from stage {resume_stage}")
        if resume_stage == '11':
            help_request_turn = input("Enter 'y' if you haven't entered the actual game. Otherwise, enter the number of help requests made: \n")
            print_timestamp(f"Will resume with written {help_request_turn} in resume.txt")
            with open("resume.txt", "w") as f:
                f.write(help_request_turn)
                time.sleep(0.2)
    else:
        with open("resume.txt", "w") as f:
            f.write("")

    if condition in prompt_files:
        prompt_file = prompt_files[condition]
        if condition == '0':  # emotional help
            help_request_file = "emotional_help_request.txt"
        elif condition == '1':  # mechanical help
            help_request_file = "mechanical_help_request.txt"
        elif condition == '2':  # emotional disclosure
            help_request_file = "emotional_help_request.txt"
        elif condition == '3':  # mechanical disclosure
            help_request_file = "mechanical_help_request.txt"

        misty_robot = MistyRobot("ENTER MISTY IP ADDRESS HERE", condition)
    else:
        print_timestamp("Invalid input. Please enter 0, 1, 2, or 3.")
