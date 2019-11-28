#import <Cordova/CDVPlugin.h>
#import <MessageUI/MessageUI.h>
#import <CoreTelephony/CTCallCenter.h>
#import <CoreTelephony/CTCall.h>
#import <CallKit/CXCallObserver.h>
#import <CallKit/CXCall.h>

@interface TELAndSMS : CDVPlugin<MFMessageComposeViewControllerDelegate,CXCallObserverDelegate>
@property(nonatomic,assign)BOOL inBecomeActive;
@property(nonatomic,assign)BOOL inBackground;
@property(nonatomic,assign)BOOL inPhoneCall;
@property(nonatomic,retain)NSTimer *timer;
@property(nonatomic,retain)NSString  *callbackid;
@property(nonatomic,strong)NSDate *dateTime;
@property(nonatomic,strong)CTCallCenter *callCenter;
@property(nonatomic,strong)CXCallObserver *callObserver;
@end
