//
//  LivePlayerViewController.h
//
//  Created by xwang on 09/05/16.
//
//

#import <UIKit/UIKit.h>
#import "NELivePlayer.h"
#import "NELivePlayerController.h"
#import "CDVLivePlayer.h"

@interface LivePlayerViewController : UIViewController

@property(nonatomic, strong) NSURL *url;
@property(nonatomic, strong) NSString *streamingTitle;
@property(nonatomic, strong) id<NELivePlayer> player;

- (id)initWithURL:(NSURL *)url title:(NSString *)title;
- (void)addChannelName:(NSString *)name andMessage:(NSString *)message;
- (void)setCommandDelegate:(CDVLivePlayer *)delegate andMessageCallbackId:(NSString *)callbackId;

@end