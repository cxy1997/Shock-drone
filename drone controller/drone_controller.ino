#define LEFT_AHEAD 12  //左轮向前转
#define LEFT_BACK 13  //左轮向后转
#define RIGHT_AHEAD 9  //右轮向前转
#define RIGHT_BACK 10  //左轮向后转
int RUN_LEFTWHEEL = 300;  //左轮前进速度
int RUN_RIGHTWHEEL = 300;  //右轮前进速度
int TURN_LEFTWHEEL = 200;  //左轮转向速度
int TURN_RIGHTWHEEL = 200;  //右轮转向速度
void headQ();  //向左前方前进
void headW();  //前进
void headE();  //向右前方前进
void headA();  //左转
void headS();  //停止
void headD();  //右转
void headZ();  //向左后方倒车
void headX();  //后退
void headC();  //向右后方倒车
void setup()  //初始化
{
    Serial.begin(9600);//打开串口
    pinMode(LEFT_AHEAD,OUTPUT);
    pinMode(LEFT_BACK, OUTPUT);
    pinMode(RIGHT_AHEAD, OUTPUT);
    pinMode(RIGHT_BACK, OUTPUT);
    digitalWrite(LEFT_AHEAD, LOW);
    digitalWrite(LEFT_BACK, LOW);
    digitalWrite(RIGHT_AHEAD, LOW);
    digitalWrite(RIGHT_BACK, LOW);
}
char signal = ' ';  //每次读入一位字符
void loop()  //主循环
{
    if (Serial.available() > 0)
    {
        signal = Serial.read();
        if (signal == 'Q') headQ(); 
        else if (signal == 'W') headW();
        else if (signal == 'E') headE();
        else if (signal == 'A') headA();
        else if (signal == 'S') headS();
        else if (signal == 'D') headD();
        else if (signal == 'Z') headZ();
        else if (signal == 'X') headX();
        else if (signal == 'C') headC();
    }
}
void headQ()  //向左前方前进
{
    digitalWrite(RIGHT_BACK,LOW);
    digitalWrite(LEFT_BACK,LOW);
    analogWrite(RIGHT_AHEAD,RUN_RIGHTWHEEL);
    analogWrite(LEFT_AHEAD,LOW);
}
void headW()  //前进
{
    digitalWrite(RIGHT_BACK,LOW);
    digitalWrite(LEFT_BACK,LOW);
    analogWrite(RIGHT_AHEAD,RUN_RIGHTWHEEL);
    analogWrite(LEFT_AHEAD,RUN_LEFTWHEEL);
}
void headE()  //向右前方前进
{
    digitalWrite(RIGHT_BACK,LOW);
    digitalWrite(LEFT_BACK,LOW);
    analogWrite(RIGHT_AHEAD,LOW);
    analogWrite(LEFT_AHEAD,RUN_LEFTWHEEL);
}
void headA()  //左转
{
    digitalWrite(RIGHT_BACK,LOW);
    digitalWrite(LEFT_AHEAD,LOW);
    analogWrite(RIGHT_AHEAD,TURN_RIGHTWHEEL);
    analogWrite(LEFT_BACK,TURN_LEFTWHEEL);
}
void headS()  //停止
{
    digitalWrite(LEFT_BACK, LOW);
    digitalWrite(RIGHT_BACK, LOW);
    digitalWrite(LEFT_AHEAD, LOW);
    digitalWrite(RIGHT_AHEAD, LOW);
}
void headD()  //右转
{
    digitalWrite(LEFT_BACK,LOW);
    digitalWrite(RIGHT_AHEAD,LOW);
    analogWrite(LEFT_AHEAD,TURN_LEFTWHEEL);
    analogWrite(RIGHT_BACK,TURN_RIGHTWHEEL);
}
void headZ()  //向左后方倒车
{
    digitalWrite(RIGHT_AHEAD,LOW);
    digitalWrite(LEFT_AHEAD,LOW);
    analogWrite(RIGHT_BACK,RUN_RIGHTWHEEL);
    analogWrite(LEFT_BACK,LOW);
}
void headX()  //后退
{
    digitalWrite(RIGHT_AHEAD,LOW);
    digitalWrite(LEFT_AHEAD,LOW);
    analogWrite(RIGHT_BACK,RUN_RIGHTWHEEL);
    analogWrite(LEFT_BACK,RUN_LEFTWHEEL);
}
void headC()  //向右后方倒车
{
    digitalWrite(RIGHT_AHEAD,LOW);
    digitalWrite(LEFT_AHEAD,LOW);
    analogWrite(RIGHT_BACK,LOW);
    analogWrite(LEFT_BACK,RUN_LEFTWHEEL);
}
