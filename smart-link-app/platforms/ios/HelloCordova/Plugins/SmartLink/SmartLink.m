/********* SmartLink.m Cordova Plugin Implementation *******/

#import <Cordova/CDV.h>
//#import "smartlinklib_7x.h"
#import "HFSmartLink.h"
#import "HFSmartLinkDeviceInfo.h"
#import <SystemConfiguration/CaptiveNetwork.h>

@interface SmartLink : CDVPlugin {
    // Member variables go here.
    HFSmartLink * smtlk;
    BOOL isconnecting;
}

- (void)coolMethod:(CDVInvokedUrlCommand*)command;
- (void) getSSID:(CDVInvokedUrlCommand*)command;
- (void)savePswd:(CDVInvokedUrlCommand*)command;
- (NSString *)getspwdByssid:(NSString * )mssid;
- (void)connect:(CDVInvokedUrlCommand*)command;

@end

@implementation SmartLink

- (void)pluginInitialize
{
    // Do any additional setup after loading the view, typically from a nib.
    smtlk = [HFSmartLink shareInstence];
    smtlk.isConfigOneDevice = true;
    smtlk.waitTimers = 30;
    isconnecting=false;
}


- (id)fetchSSIDInfo {
    NSArray *ifs = (__bridge_transfer id)CNCopySupportedInterfaces();
    NSLog(@"Supported interfaces: %@", ifs);
    id info = nil;
    for (NSString *ifnam in ifs) {
        info = (__bridge_transfer id)CNCopyCurrentNetworkInfo((__bridge CFStringRef)ifnam);
        NSLog(@"%@ => %@", ifnam, info);
        if (info && [info count]) { break; }
    }
    return info;
}

- (void) getSSID:(CDVInvokedUrlCommand*)command {
	BOOL wifiOK= FALSE;
    NSDictionary *ifs;
    NSString *ssid;
    CDVPluginResult* pluginResult = nil;

	if (!wifiOK)
    {
        ifs = [self fetchSSIDInfo];
        ssid = [ifs objectForKey:@"SSID"];
        
        NSLog(@"SSID是： %@", ssid);
        
        if (ssid!= nil)
        {
            wifiOK= TRUE;
        
            pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:ssid];
            
        } else {
            pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR];
        }
        
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
        
    }

}


- (void)savePswd:(CDVInvokedUrlCommand*)command {
	NSDictionary *params = [command.arguments objectAtIndex:0];

    NSUserDefaults * def = [NSUserDefaults standardUserDefaults];
    [def setObject:[params objectForKey:@"wifiPass"] forKey:[params objectForKey:@"wifiName"]];
}

- (NSString *)getspwdByssid:(NSString * )mssid{
    NSUserDefaults * def = [NSUserDefaults standardUserDefaults];
    return [def objectForKey:mssid];
}

- (void)connect:(CDVInvokedUrlCommand*)command {
	NSDictionary *params = [command.arguments objectAtIndex:0];
	NSString * ssidStr= [params objectForKey:@"wifiName"];
    NSString * pswdStr = [params objectForKey:@"wifiPass"];
    
    if(!isconnecting){
        isconnecting = true;

    	// Check command.arguments here.
    	[self.commandDelegate runInBackground:^{
	        [smtlk startWithSSID:ssidStr Key:pswdStr withV3x:true
	                processblock: ^(NSInteger pro) {
	                    // todo: in progress
	                } successBlock:^(HFSmartLinkDeviceInfo *dev) {
	                	[self.commandDelegate sendPluginResult:[NSString stringWithFormat:@"{status:1, mac: '%@', ip: '%@'}",dev.mac,dev.ip] callbackId:command.callbackId];
	                } failBlock:^(NSString *failmsg) {
	                    
	                	[self.commandDelegate sendPluginResult:[NSString stringWithFormat:@"{status:0, error: '%@'}", failmsg] callbackId:command.callbackId];

	                } endBlock:^(NSDictionary *deviceDic) {
	                    isconnecting  = false;
	                }];
	    }];
    }else{
        [smtlk stopWithBlock:^(NSString *stopMsg, BOOL isOk) {
            if(isOk){
                isconnecting  = false;
                [self showAlertWithMsg:stopMsg title:@"Stopped OK"];
            }else{
                [self showAlertWithMsg:stopMsg title:@"Stop with error"];
            }
        }];
    }

}


-(void)showAlertWithMsg:(NSString *)msg
                  title:(NSString*)title{
    UIAlertView * alert = [[UIAlertView alloc]initWithTitle:title message:msg delegate:nil cancelButtonTitle:@"cancel" otherButtonTitles:@"ok", nil];
    [alert show];
}


- (void)coolMethod:(CDVInvokedUrlCommand*)command
{
    CDVPluginResult* pluginResult = nil;
    NSString* echo = [command.arguments objectAtIndex:0];

    if (echo != nil && [echo length] > 0) {
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:echo];
    } else {
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR];
    }

    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}



@end
