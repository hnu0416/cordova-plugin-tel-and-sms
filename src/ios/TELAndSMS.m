#import "TELAndSMS.h"

@implementation TELAndSMS

- (void)pluginInitialize{
    self.inBecomeActive = NO;
    self.inBackground = NO;
    [[NSNotificationCenter defaultCenter] addObserver:self
                                             selector:@selector(resignActive)
                                                 name:UIApplicationWillResignActiveNotification
                                               object:nil];
    [[NSNotificationCenter defaultCenter] addObserver:self
                                             selector:@selector(enterBackground)
                                                 name:UIApplicationDidEnterBackgroundNotification
                                               object:nil];
    [[NSNotificationCenter defaultCenter] addObserver:self
                                             selector:@selector(becomeActive)
                                                 name:UIApplicationDidBecomeActiveNotification object:nil];
    if([UIDevice currentDevice].systemVersion.floatValue >=10.0){
        [self initPhoneStateIniOS10];
    }else{
        [self initPhoneState];
    }
}

- (void)initPhoneStateIniOS10{
    _callObserver = [[CXCallObserver alloc] init];
    [_callObserver setDelegate:self queue:dispatch_get_main_queue()];
}


- (BOOL)checkCanCall{
    CTTelephonyNetworkInfo *networkInfo = [[CTTelephonyNetworkInfo alloc] init];
    CTCarrier *carrier = networkInfo.subscriberCellularProvider;
    if (carrier.isoCountryCode == nil) {
        return NO;
    }
    if (carrier.mobileCountryCode == nil) {
        return NO;
    }
    if (carrier.mobileNetworkCode == nil){
        return NO;
    }
    if ([carrier mobileNetworkCode] == nil){
        return NO;
    }
    return YES;
}

-(void)callObserver:(CXCallObserver *)callObserver callChanged:(CXCall *)call{
    if(call.outgoing && call.isOnHold){
        [self callbackHandle:@"0" andStatus:CDVCommandStatus_OK];
    }
    if (call.outgoing && call.hasConnected && !call.hasEnded) {
        _dateTime = [NSDate date];
    }
    if (call.outgoing && call.hasConnected && call.hasEnded) {
        [self callbackHandle:[self calculatePhoneCallTime] andStatus:CDVCommandStatus_OK];
    }
}

- (NSString *)calculatePhoneCallTime{
    NSDate* dat = [NSDate dateWithTimeInterval:0 sinceDate:_dateTime];
    NSTimeInterval a=[dat timeIntervalSinceNow];
    return [NSString stringWithFormat:@"%0.f",fabs(a)];
}

- (void)initPhoneState{
    CTCallCenter *callCenter = [[CTCallCenter alloc] init];
    callCenter.callEventHandler = ^(CTCall* call) {
        if ([call.callState isEqualToString:CTCallStateDisconnected]){
            [self callbackHandle:[self calculatePhoneCallTime] andStatus:CDVCommandStatus_OK];
        }else if ([call.callState isEqualToString:CTCallStateConnected]){
            _dateTime = [NSDate date];
        }
    };
}

- (void)callTel:(CDVInvokedUrlCommand *)command{
    self.callbackid = command.callbackId;
    NSMutableString * string = [[NSMutableString alloc] initWithFormat:@"tel://%@",[NSString stringWithFormat:@"%@",command.arguments[0]]];
    if(![self checkCanCall]){
        [self showNotNetError];
        return;
    }
    if ([[UIDevice currentDevice].systemVersion floatValue] >= 10.0) {
        [[UIApplication sharedApplication] openURL:[NSURL URLWithString:string] options:@{} completionHandler:nil];
    }else{
        UIAlertController *alertController = [UIAlertController alertControllerWithTitle:nil message:command.arguments[0] preferredStyle:UIAlertControllerStyleAlert];
        UIAlertAction *cancelAction = [UIAlertAction actionWithTitle:@"取消" style:UIAlertActionStyleCancel handler:^(UIAlertAction *action) {
            [self callbackHandle:@"取消拨号" andStatus:CDVCommandStatus_ERROR];
        }];
        UIAlertAction *otherAction = [UIAlertAction actionWithTitle:@"呼叫" style:UIAlertActionStyleDefault handler:^(UIAlertAction *action) {
            [[UIApplication sharedApplication] openURL:[NSURL URLWithString:string]];
        }];
        [alertController addAction:cancelAction];
        [alertController addAction:otherAction];
        [self.viewController presentViewController:alertController animated:YES completion:nil];
    }
}
- (void)enterBackground{
    self.inBackground = YES;
    [self callbackHandle:@"0" andStatus:CDVCommandStatus_OK];
}
- (void)resignActive{
    if(self.inBecomeActive){
        [self.timer invalidate];
        self.timer = nil;
        self.inPhoneCall = YES;
    }
}
- (void)becomeActive {
    if(self.inBackground){
        self.inBackground = NO;
        return;
    }
    if(self.inPhoneCall){
        self.inPhoneCall = NO;
        self.inBecomeActive = NO;
        return;
    }
    self.inBecomeActive = YES;
    self.timer = [NSTimer scheduledTimerWithTimeInterval:1.5 target:self selector:@selector(setInBecomeActiveNo) userInfo:nil repeats:NO];
}
- (void)setInBecomeActiveNo{
    self.inBecomeActive = NO;
    [self callbackHandle:@"0" andStatus:CDVCommandStatus_ERROR];
}
- (void)callSMS:(CDVInvokedUrlCommand *)command{
    self.callbackid = command.callbackId;
    NSDictionary *smsObj = command.arguments[0];
    if(![self checkCanCall]){
        [self showNotNetError];
        return;
    }
    if([MFMessageComposeViewController canSendText]){
        MFMessageComposeViewController * controller = [[MFMessageComposeViewController alloc] init];
        controller.recipients = [smsObj objectForKey:@"phones"];
        controller.navigationBar.tintColor = [UIColor redColor];
        controller.body = [smsObj objectForKey:@"content"];
        controller.messageComposeDelegate = self;
        [self.viewController presentViewController:controller animated:YES completion:nil];
        [[[[controller viewControllers] lastObject] navigationItem] setTitle:@"发送短信"];
    }else{
        UIAlertController *alertController = [UIAlertController alertControllerWithTitle:nil message:@"该设备不支持短信功能" preferredStyle:UIAlertControllerStyleAlert];
        [self.viewController presentViewController:alertController animated:YES completion:nil];
    }
}
-(void)messageComposeViewController:(MFMessageComposeViewController *)controller didFinishWithResult:(MessageComposeResult)result{
    [self.viewController dismissViewControllerAnimated:YES completion:nil];
    switch (result) {
        case MessageComposeResultSent:
            [self callbackHandle:@"发送成功" andStatus:CDVCommandStatus_OK];
            break;
        case MessageComposeResultFailed:
            [self callbackHandle:@"发送失败" andStatus:CDVCommandStatus_ERROR];
            break;
        case MessageComposeResultCancelled:
            [self callbackHandle:@"发送用户取消发送" andStatus:CDVCommandStatus_ERROR];
            break;
        default:
            break;
    }
}

- (void)showNotNetError{
    UIAlertController *alertController = [UIAlertController alertControllerWithTitle:nil message:@"没有移动网络" preferredStyle:UIAlertControllerStyleAlert];
    UIAlertAction *cancelAction = [UIAlertAction actionWithTitle:@"取消" style:UIAlertActionStyleCancel handler:^(UIAlertAction *action) {
        [self callbackHandle:@"没有移动网络" andStatus:CDVCommandStatus_ERROR];
    }];
    [alertController addAction:cancelAction];
    [self.viewController presentViewController:alertController animated:YES completion:nil];
}

- (void)callbackHandle:(NSString *)message andStatus:(CDVCommandStatus)status {
    [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:status messageAsDictionary:@{@"message": message}] callbackId: self.callbackid];
    self.callbackid = nil;
}
@end
