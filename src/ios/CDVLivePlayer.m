//
//  CDVLivePlayer.m
//
//  Created by xwang on 09/05/16.
//
//

#import "CDVLivePlayer.h"
#import "LivePlayerViewController.h"

@implementation CDVLivePlayer:CDVPlugin

- (void)play:(CDVInvokedUrlCommand *)command
{
  NSArray *arguments = [command arguments];
  if ([arguments count] != 3) {
    [self failWithCallbackId:command.callbackId withMessage:@"参数错误"];
    return;
  }

  NSURL *url = [[NSURL alloc] initWithString:[arguments objectAtIndex:0]];
  NSString *title = [arguments objectAtIndex:1];
  BOOL onSchedule = [[arguments objectAtIndex:2] boolValue];

  LivePlayerViewController *playerViewController = [[LivePlayerViewController alloc] initWithURL:url title:title andOnSchedule:onSchedule];
  if (playerViewController == nil) {
    [self failWithCallbackId:command.callbackId withMessage:@"初始化错误"];
    return;
  }

  [self.viewController presentViewController:playerViewController animated:NO completion:nil];
  [self successWithCallbackId:command.callbackId withMessage:@"大丈夫"];
}

#pragma mark Helper Function
- (void)successWithCallbackId:(NSString *)callbackId withMessage:(NSString *)message
{
  CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:message];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:callbackId];
}

- (void)failWithCallbackId:(NSString *)callbackId withMessage:(NSString *)message
{
  CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:message];
  [self.commandDelegate sendPluginResult:pluginResult callbackId:callbackId];
}

@end