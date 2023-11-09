# "Embedded Sentry"

## 1. Successful Accomplishments
In this project, we utilized the STM429 hardware facility to develop a system that records and replicates board movements for unlocking purposes. We have successfully implemented the following functionalities:
### 1) Movement Recording:
   The system allows users to record specific board movements by utilizing the hardware. These movements are captured by the gyroscope and stored for future use.
### 2) Pattern Replication:
   By repeating the recorded movement pattern, the system is capable of unlocking the board. It compares the user's input with the recorded pattern and grants access if they match.
   
## 2. Device Usage Instruction
### 1) Start:
   Upon powering on the device, a screen prompt will appear, instructing the user to press the blue button to record the desired pattern.
### 2) Pattern Recording:
   Following the on-screen instructions, hold down the blue button to begin recording the movement pattern. The screen will display "recording" while the button is pressed. The pattern should be completed within 4 seconds.
### 3) Recording Confirmation: 
Once the recording is complete, release the button. The screen will display "recorded" to confirm that the pattern has been successfully recorded. After this message disappears, the instruction will change to "Press Blue button to Unlock pattern".
### 4) Pattern Unlocking:
   To unlock the board, press and hold the blue button again, inputting the same movement pattern that was recorded.
   - Unlock Success: If the inputted pattern matches the recorded one closely, the screen will turn green and display "Unlock Successful."
   - Unlock Failure: If the inputted pattern differs significantly from the recorded one, the screen will turn red and display "Unlock Failed."
   - **What can be seen as similar:** similar shape with similar speed—not much faster or slower than the original recorded one.

## 3. Algorithm and Implementation Steps
The following algorithm and steps were employed to accomplish the functionalities mentioned above:
### 1) Gyroscope Data Recording:
   We retrieved gyroscope readings by referencing a demo in our class. The x, y, and z values were extracted and passed to the main function for further processing.
### 2) Data Smoothing:
   To enhance the accuracy of the recorded movements, we applied a moving average filter to the gyroscope data. This filtering technique helped to eliminate noise and ensure smoother patterns.
### 3) Pattern Comparison:
   We utilized two approaches for comparing the recorded pattern with the inputted one, depending on their complexity:
   - Simple Patterns: For straightforward patterns, we calculated the distance between the original pattern and the inputted one. This method proved effective in simpler scenarios.
   - Complex Patterns: When dealing with intricate patterns, we employed cosine similarity to compare the recorded and inputted patterns on each dimension. This approach provided better accuracy and reliability.
6) User Feedback: We utilized functions available on the LCD screen to provide clear instructions to the user. By using different colors and text, we visually communicated whether the inputted pattern was correct or not. Green indicated a successful unlock, while red denoted a failed attempt.
