//
//  CDVLivePlayer.h
//
//  Created by xwang on 09/05/16.
//
//

#import <Cordova/CDV.h>

@interface CDVLivePlayer:CDVPlugin

- (void)play:(CDVInvokedUrlCommand *)command;
- (void)channel:(CDVInvokedUrlCommand *)command;
- (void)message:(CDVInvokedUrlCommand *)command;
- (void)successWithCallbackId:(NSString *)callbackId withMessage:(NSString *)message andKeep:(BOOL)keep;

@end