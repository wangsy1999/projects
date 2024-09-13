import tkinter as tk
from tkinter import messagebox
from tkinter import ttk
import cv2
import mediapipe as mp
import pyaudio
import struct
import threading
import numpy as np
from scipy import signal
from math import sin, cos, pi


# there are serveral different fuctions to do some specific jobs. I will label the position of them here. 
# There will be "display_help" just below these comments for showing a help text when click "help" in GUI.
# The function for TTheremin is at line 30. Please notice that if the window is too big, please adjust the resolution in that function. 
# The function for "Air Key" is at line 216. 
# The function for simple "Keyboard" is at line 417. 
# The function for GUI is at line 531.

def display_help():
    # Help text
    help_text = "This is the help text for the application.\n\n" \
                "Theremin: Use your right hand to control the frequency of the sound, left hand to change the volume.\n\n" \
                "Air Key: Simply use your index finger of right hand, touch the air, play the keys.\n\n" \
                "Keyboard: Use keys including 'a', 'w', 's', 'e', 'd', 'f', 't', 'g', 'y', 'h', 'u', 'j' to play notes from C4 to B4."
    messagebox.showinfo("Help", help_text)

# Function for Theremin
def run_TM():
    global theta,run_threads,frequency,gain,previous_gain,RATE,BLOCKLEN,cap, p, stream, hands
    RATE = 8000
    BLOCKLEN = 128
    previous_gain=0
    gain = 0
    frequency = 0
    theta = 0
    run_threads = True
    
    p = pyaudio.PyAudio()
    stream = p.open(
        format=pyaudio.paInt16,
        channels=1,
        rate=RATE,
        input=False,
        output=True,
        frames_per_buffer=BLOCKLEN)
    
    # Initialize MediaPipe and Webcam
    mp_hands = mp.solutions.hands
    mp_drawing = mp.solutions.drawing_utils
    hands = mp_hands.Hands(
        static_image_mode=False,
        max_num_hands=2,
        min_detection_confidence=0.5)
    cap = cv2.VideoCapture(0)
    #cv2.namedWindow('Hand Tracking')
    def on_closing(): # the function to run when close the window
        global run_threads   
        run_threads = False # stop the thread
        global cap  # Ensure this is the correct reference to your camera object
        if cap is not None:
            cap.release()
        root.destroy()

    root = tk.Tk()
    root.title("Theremin")
    tip_label = tk.Label(root, text="Theremin",font=("Helvetica", 14))
    tip_label.pack()
    frequency_label = tk.Label(root, text="Frequency: 0 Hz")
    frequency_label.pack()
    gain_label = tk.Label(root, text="Gain: 0")
    gain_label.pack()
    # Set the dimensions of the window, If your window is too big, please adjust it
    window_width = 800
    window_height = 500
    root.geometry(f"{window_width}x{window_height+200}")
    red_label = tk.Label(root, text="Red: Right Wrist (Frequency)", fg="red") # legend for right hand
    red_label.pack()

    blue_label = tk.Label(root, text="Blue: Left Wrist (Gain)", fg="blue")  # legend for right hand
    blue_label.pack()
    # Create a canvas for drawing
    canvas = tk.Canvas(root, width=window_width, height=window_height)  # canvas to show the position
    canvas.pack()
    exit_button = tk.Button(root, text="Exit", font=("Helvetica", 16), command=on_closing)
    exit_button.pack(pady=20)

    # Lock for synchronizing access to shared variables
    variables_lock = threading.Lock()
    run_threads = True
    # Initialize PyAudio

    
    # Function for video processing and hand tracking
    def video_processing():
        global frequency, gain, previous_gain,run_threads

        while run_threads and cap.isOpened():
            ret, frame = cap.read()
            if not ret:
                break
            #cv2.namedWindow('Hand Tracking')
            #frame = cv2.resize(frame, (800, 600))
            frame = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
            results = hands.process(frame)
            left_hand_found = False # initialize the hand status
            right_hand_found = False
    
            with variables_lock:
                local_frequency = frequency
                local_gain = gain
                
            try:
                canvas.delete("all")
                    # Further updates to the canvas
            except tk.TclError:
                break  
                            
    
            if results.multi_hand_landmarks:
                for hand_idx, hand_landmarks in enumerate(results.multi_hand_landmarks):
                    if results.multi_handedness:
                        hand_label = results.multi_handedness[hand_idx].classification[0].label
                        
    
                        if hand_label == 'Left': # That is not a mistake, we tested it. 
                            right_wrist = hand_landmarks.landmark[5]
                            local_frequency = 200 + (1 - right_wrist.x) * 500 * 3  # Define the relationship between right hand and frequency
                            mrR=1 - (right_wrist.x)
                            #print(mrR)
                            right_hand_found = True
                            if run_threads:
                                canvas.create_oval(mrR * window_width - 5, right_wrist.y * window_height - 5, mrR * window_width + 5, right_wrist.y * window_height + 5, fill="red") 
                        elif hand_label == 'Right':
                            left_wrist = hand_landmarks.landmark[5]
                            local_gain = left_wrist.y *15000  # Define the relationship between left hand and gain
                            left_hand_found = True
                            mrL=1-left_wrist.x
                            if run_threads:
                                canvas.create_oval(mrL * window_width - 5, left_wrist.y * window_height - 5, mrL * window_width + 5, left_wrist.y * window_height + 5, fill="blue")
    
            if not (left_hand_found and right_hand_found): # set gain to 0 if missed any hands
                local_gain = 0.0
    
            with variables_lock:
                frequency = local_frequency
                gain = local_gain
            frame = cv2.cvtColor(frame, cv2.COLOR_RGB2BGR)
            if results.multi_hand_landmarks:
                for hand_landmarks in results.multi_hand_landmarks:
                    mp_drawing.draw_landmarks(frame, hand_landmarks, mp_hands.HAND_CONNECTIONS)

            #frame_flipped = cv2.flip(frame, 1)
            #cv2.imshow('Hand Tracking', frame_flipped)
            if cv2.waitKey(1) & 0xFF == 27:
                run_threads = False
                break

            try: # in case that the update is still run even if the window is closed
                frequency_label.config(text=f"Frequency: {frequency:.2f} Hz")
                gain_label.config(text=f"Gain: {gain:.2f}")
                root.update() 
            except tk.TclError:
                break   
                
    # play the sound
    def audio_processing():
        global run_threads,previous_gain,theta

        while run_threads and cap.isOpened():
            with variables_lock:
                local_frequency = frequency
                local_gain = gain

            gain_step = (local_gain - previous_gain) / BLOCKLEN # smooth the audio to avoid clipping 
            om1 = 2.0 * pi * local_frequency / RATE
            signal_block = [0] * BLOCKLEN
    
            for i in range(BLOCKLEN):
                A = previous_gain + gain_step * i
                signal_block[i] = int(A  * cos(theta))
                if signal_block[i] > 32767: # clip the sound so that it will be 16 bits
                    signal_block[i] = 32767
                elif signal_block[i] < -32768:
                    signal_block[i] = -32768
                theta += om1
                while theta > pi:
                    theta -= 2.0 * pi
    
            previous_gain = local_gain
            binary_data = struct.pack('h' * BLOCKLEN, *signal_block)
            stream.write(binary_data)
    
    root.protocol("WM_DELETE_WINDOW", on_closing) # the function to run when close the window

    # Start threads
    video_thread = threading.Thread(target=video_processing)
    audio_thread = threading.Thread(target=audio_processing)
    
    video_thread.start()
    audio_thread.start()
    # Wait for threads to complete
    root.mainloop()
    
    video_thread.join()
    audio_thread.join()
    # Cleanup
    cap.release()
    cv2.destroyAllWindows()
    stream.stop_stream()
    stream.close()
    p.terminate()
    
#function for air key
def run_AK():
    global press,run_threads,frequency,gain,previous_gain,theta,RATE,BLOCKLEN
    theta=0
    gain = 0
    press=0
    frequency = 0
    previous_gain = 0
    RATE = 8000
    BLOCKLEN = 128
    run_threads = True
    variables_lock = threading.Lock()
    p = pyaudio.PyAudio()
    stream = p.open(
        format=pyaudio.paInt16,
        channels=1,
        rate=RATE,
        input=False,
        output=True,
        frames_per_buffer=BLOCKLEN)
    
    # Initialize MediaPipe and Webcam
    mp_hands = mp.solutions.hands
    mp_drawing = mp.solutions.drawing_utils
    hands = mp_hands.Hands(
        static_image_mode=False,
        max_num_hands=2,
        min_detection_confidence=0.5)
    cap = cv2.VideoCapture(0)
    
    note_frequencies = {
        'C4': 261.63, 'D4': 293.66, 'E4': 329.63, 'F4': 349.23,  # a map for frequency of the air keys
        'G4': 392.00, 'A4': 440.00, 'B4': 493.88
    }
    notes = list(note_frequencies.keys())
    
    def get_note_from_position(x_position): # find which key is the right hand at
        num_notes = len(notes)
        note_index = int(x_position * num_notes)
        note_index = min(note_index, num_notes - 1)  # Ensure index is within range
        return notes[note_index]
    
    # Function for video processing and hand tracking
    def video_processing():
        global frequency, gain, previous_gain,run_threads,press
        # define the size of the keys
        block_width = 127
        block_height = 600
        block_y = 100  # Y-coordinate of all blocks
        block_spacing = 10 
        transparency = 0.5  # Transparency factor
        # set threshold
        press_on_counter = 0
        press_off_counter = 0
        press_threshold_off = 8
        press_threshold_on = 8
        note_counters = {note: 0 for note in notes}  
        note_threshold = 12
        z_threshold = 0.055  
        selected_note = None  
            
        while run_threads and cap.isOpened():
            ret, frame = cap.read()
            if not ret:
                break
            RF=0
            frame = cv2.resize(frame, (1000, 800))
            frame = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
            results = hands.process(frame)
            current_note = None
            highlighted_note = None
            with variables_lock:
                local_frequency = frequency
                local_gain = gain
    
            if results.multi_hand_landmarks:
                for hand_idx, hand_landmarks in enumerate(results.multi_hand_landmarks):
                    if results.multi_handedness:
                        hand_label = results.multi_handedness[hand_idx].classification[0].label
                        
    
                        if hand_label == 'Left':
                            right_wrist = hand_landmarks.landmark[8]
                            current_note = get_note_from_position(1-right_wrist.x)
                            RF=1
                            
                            if -right_wrist.z >= z_threshold: # if pressing time longer than threshold, it is pressed
                                press_on_counter += 1
                                press_off_counter = 0
                                if press_on_counter >= press_threshold_on:
                                    press = 1
                                    
                            else:
                                press_off_counter += 1 # # if released time longer than threshold, it is released
                                press_on_counter = 0
                                if press_off_counter >= press_threshold_off:
                                    press = 0
                        
                        if press ==1:
                            highlighted_note = selected_note
                        
                        if RF==0:
                                press = 0
                       
                        for note in notes: 
                            if note == current_note: 
                                note_counters[note] += 1  # in case some identification error will suddenly change a key
                                if note_counters[note] >= note_threshold and selected_note != note: 
                                    selected_note = note
                                    local_frequency = note_frequencies[note]
                            
                            else:
                                note_counters[note] = 0  
                               
            else:
                
                    press = 0     

            with variables_lock:
                frequency = local_frequency
                gain = local_gain
        
            frame = cv2.cvtColor(frame, cv2.COLOR_RGB2BGR)
            #if results.multi_hand_landmarks:
                #for hand_landmarks in results.multi_hand_landmarks:
                    #mp_drawing.draw_landmarks(frame, hand_landmarks, mp_hands.HAND_CONNECTIONS)
    
            frame_flipped = cv2.flip(frame, 1)
            overlay = frame_flipped.copy()
            # put keys on the camera stream
            for i, note in enumerate(notes):
                block_x = i * (block_width + block_spacing) + 20  # X-coordinate of each block
                block_color = (0, 255, 0) if note == highlighted_note else (255, 0, 0)
    
            # Draw semi-transparent rectangles on the overlay
                cv2.rectangle(overlay, (block_x, block_y), (block_x + block_width, block_y + block_height), block_color, -1)
                cv2.putText(overlay, note, (block_x + 10, block_y + 30), cv2.FONT_HERSHEY_SIMPLEX, 0.7, (255, 255, 255), 2)
    
        # Blend the overlay with the frame
            cv2.addWeighted(overlay, transparency, frame_flipped, 1 - transparency, 0, frame_flipped)
            text = "Press ESC to exit"  # put texts on the video window 
            position = (250, 50)  
            font = cv2.FONT_HERSHEY_SIMPLEX
            font_scale = 1
            font_color = (0, 255, 0)  
            line_type = 3
            cv2.putText(frame_flipped, text, position, font, font_scale, font_color, line_type)
            cv2.imshow('Air Key', frame_flipped)
            if cv2.waitKey(1) & 0xFF == 27:
                run_threads = False
                break
    
    # Function for audio processing
    def audio_processing():
        global run_threads,previous_gain,theta,press
        while run_threads and cap.isOpened():
            with variables_lock:
                local_frequency = frequency
                local_gain = gain
            if press==0:
                local_gain = 0
            else:
                local_gain = 15000 
            # same sound function as privious one
            gain_step = (local_gain - previous_gain) / BLOCKLEN
            om1 = 2.0 * pi * local_frequency / RATE     
            signal_block = [0] * BLOCKLEN
            
            for i in range(BLOCKLEN):
                A = previous_gain + gain_step * i
                signal_block[i] = int(A * cos(theta))
                if signal_block[i] > 32767:
                    signal_block[i] = 32767
                elif signal_block[i] < -32768:
                    signal_block[i] = -32768
                theta += om1
                while theta > pi:
                    theta -= 2.0 * pi
    
            previous_gain = local_gain
            binary_data = struct.pack('h' * BLOCKLEN, *signal_block)
            stream.write(binary_data)
    
    # Start threads
    video_thread = threading.Thread(target=video_processing)
    audio_thread = threading.Thread(target=audio_processing)
    
    video_thread.start()
    audio_thread.start()
    
    # Wait for threads to complete
    video_thread.join()
    audio_thread.join()
    
    # Cleanup
    cap.release()
    cv2.destroyAllWindows()
    stream.stop_stream()
    stream.close()
    p.terminate()
    
#function for keyboard, some of them from the course demo
def run_KB():
    BLOCKLEN = 64   # Number of frames per block
    WIDTH = 2       # Bytes per sample
    CHANNELS = 1    # Mono
    RATE = 8000     # Frames per second
    MAXVALUE = 2**15 - 1  # Maximum allowed output signal value (because WIDTH = 2)

    # Parameters
    Ta = 1      # Decay time (seconds)
    f1 = 450    # Frequency (Hz)
    r = 0.01**(1.0/(Ta*RATE))  # 0.01 for 1 percent amplitude
    om1 = 2.0 * pi * float(f1)/RATE
    a = [1, -2*r*cos(om1), r**2]
    b = [r*sin(om1)]
    ORDER = 2
    states = np.zeros(ORDER)
    x = np.zeros(BLOCKLEN)

    # Open the audio output stream
    p = pyaudio.PyAudio()
    PA_FORMAT = pyaudio.paInt16
    stream = p.open(
            format=PA_FORMAT,
            channels=CHANNELS,
            rate=RATE,
            input=False,
            output=True,
            frames_per_buffer=BLOCKLEN)

    CONTINUE = True
    KEYPRESS = False
    key_map  = {                # map for 12 keys
        'a': ('C4', 261.63),
        'w': ('C#4', 277.18),
        's': ('D4', 293.66),
        'e': ('D#4', 311.13),
        'd': ('E4', 329.63),
        'f': ('F4', 349.23),
        't': ('F#4', 369.99),
        'g': ('G4', 392.00),
        'y': ('G#4', 415.30),
        'h': ('A4', 440.00),
        'u': ('A#4', 466.16),
        'j': ('B4', 493.88),
        }

    def update_coefficients():
        nonlocal a, b, states, f1, r
        om1 = 2.0 * pi * float(f1) / RATE
        a = [1, -2*r*cos(om1), r**2]
        b = [r*sin(om1)]
        states = np.zeros(ORDER)

    def create_key_display():
        keys_frame = tk.Frame(root)
        keys_frame.pack()

        key_labels = {}
        for key, value in key_map.items():
            bg_color = "black" if "#" in value[0] else "white"      # set key color
            fg_color = "white" if "#" in value[0] else "black"
            label = tk.Label(keys_frame, text=value[0], width=7, height=15, bg=bg_color, fg=fg_color, relief="raised", font=("Helvetica", 15))
            label.pack(side="left", padx=10, pady=10)
            key_labels[key] = label
        return key_labels

    def key_press(event):
        nonlocal KEYPRESS, f1
        if event.char in key_map:
            f1 = key_map[event.char][1]
            update_coefficients()
            KEYPRESS = True
            key_labels[event.char].config(bg="green")       # key on screen will be green when pressed 

    def key_release(event):
        nonlocal KEYPRESS
        if event.char in key_labels:
            KEYPRESS = False
            bg_color = "black" if "#" in key_map[event.char][0] else "white"
            key_labels[event.char].config(bg=bg_color)

    def exit_program():
        nonlocal CONTINUE
        CONTINUE = False

    root = tk.Tk()
    root.bind("<KeyPress>", key_press)
    root.bind("<KeyRelease>", key_release)
    key_labels = create_key_display()

    exit_button = tk.Button(root, text="Exit", font=("Helvetica", 16),command=lambda: [exit_program(), root.destroy()])
    exit_button.pack(side="bottom", pady=10)

    while CONTINUE:
        try:
            root.update()
            if KEYPRESS and CONTINUE:
                x[0] = 10000.0
            [y, states] = signal.lfilter(b, a, x, zi=states)
            x[0] = 0.0
            KEYPRESS = False
            y = np.clip(y, -MAXVALUE, MAXVALUE)
            y_16bit = y.astype('int16')
            y_bytes = y_16bit.tobytes()
            stream.write(y_bytes, BLOCKLEN)
        except tk.TclError:
            break

    stream.stop_stream()
    stream.close()
    p.terminate()


#function for create the GUI
def create_main_gui():
    root = tk.Tk()
    root.title("Control Panel")
    window_width = 300      # set the size of the GUI
    window_height = 400     
    root.geometry(f"{window_width}x{window_height}")
    # used ttk for better looking
    style = ttk.Style()
    style.configure('Large.TButton', font=('Helvetica', 16))

    btn_pj1_1 = ttk.Button(root, text="Theremin", style='Large.TButton', command=lambda: run_TM()) # press to run the "run_TM()" function
    btn_pj1_1.pack(pady=20)

    btn_pj1_2 = ttk.Button(root, text="Air Key", style='Large.TButton', command=lambda: run_AK())
    btn_pj1_2.pack(pady=20)

    btn_pj1_3 = ttk.Button(root, text="Keyboard", style='Large.TButton', command=lambda: run_KB())
    btn_pj1_3.pack(pady=20)

    help_button = ttk.Button(root, text="Help", style='Large.TButton', command=display_help)
    help_button.pack(pady=20)

    exit_button = ttk.Button(root, text="Exit", style='Large.TButton', command=root.destroy)
    exit_button.pack(pady=20)
    root.mainloop()
# generate the GUI
create_main_gui()





