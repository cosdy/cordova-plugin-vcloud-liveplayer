//
//  LivePlayerViewController.h
//
//  Created by xwang on 09/05/16.
//
//

#import <UIKit/UIKit.h>
#import "NELivePlayer.h"
#import "NELivePlayerController.h"

@interface LivePlayerViewController : UIViewController

@property(nonatomic, strong) NSURL *url;
@property(nonatomic, strong) NSString *streamingTitle;
@property(nonatomic, strong) NSDictionary *options;
@property(nonatomic, strong) id<NELivePlayer> player;

- (id)initWithURL:(NSURL *)url title:(NSString *)title andOptions:(NSDictionary *)options;

@end