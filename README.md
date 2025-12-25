# Robot Requests for Help
*Updated: Dec. 25, 2025*

This repository contains the code used for the paper:
> **Can You Help Me? The Influence of Robot Requests for Help on Child–Robot Connection**

### Citation

> Teresa Flanagan, Justin Chenjia Zhang, Lin Bian, and Sarah Sebo. 2026. Can You Help Me? The Influence of Robot Requests for Help on Child–Robot Connection. In *Proceedings of the 2026 ACM/IEEE International Conference on Human-Robot Interaction (HRI ’26)*. ACM, New York, NY, USA, 10 pages. https://doi.org/10.1145/3757279.3785631

# Instructions for Running the Code on macOS
This repository uses the [Misty Python-SDK](https://github.com/MistyCommunity/Python-SDK), which allows direct programming of the Misty robot on macOS.
1. Clone this repository
2. Create a virtual environment and install the dependencies listed in ```req.txt```
   
   **(a) RevAI may run into an error due to version incompatibility. If this happens, please refer to the error message and update the function name in that line of the RevAI package**
3. Clone the Misty Python-SDK in your laptop:

   ```git clone git@github.com:MistyCommunity/Python-SDK.git```
4. Move the Python-SDK folder into this repository's directory. The folder structure should resemble the following:

   ```bash
   /Robot_Requests_for_Help
     ├── main.py
     ├── update.py
     ├── req.txt
     ├── /tablet-game
     ├── /Python-SDK
     └── /venv (optional, created during setup)
   ```
5. Update the misty IP address in `main.py` **line 717**
6. The ```tablet-game``` folder contains Java code for our tablet game, which should be run through Android Studio. When running the codes
    
    (a) Change the file path in ```local.properties``` into your path
    
    (b) (Optional) To avoid manually input IP address everytime, the default IP can be set in **line 27** of ```app/src/main/java/Socketconnection/TCPClient.java```

# Instructions for Running the Code on Windows
For Windows setup and instructions, please refer to the [Misty Lessons - Desktop Environment](https://lessons.mistyrobotics.com/misty-lessons/desktop-environment)

