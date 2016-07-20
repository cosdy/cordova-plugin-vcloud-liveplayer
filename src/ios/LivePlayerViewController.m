//
//  LivePlayerViewController.m
//
//  Created by xwang on 09/05/16.
//
//

#import "LivePlayerViewController.h"
#import <Photos/Photos.h>
#import <Cordova/CDV.h>

#define UIColorFromRGB(rgbValue) [UIColor colorWithRed:((float)((rgbValue & 0xFF0000) >> 16))/255.0 green:((float)((rgbValue & 0xFF00) >> 8))/255.0 blue:((float)(rgbValue & 0xFF))/255.0 alpha:1.0]

@interface LivePlayerViewController()

@property (nonatomic, strong) UIView *playerView;
@property (nonatomic, strong) UIControl *streamingOverlay;
@property (nonatomic, strong) UIControl *controlOverlay;
@property (nonatomic, strong) UIView *topControlView;
@property (nonatomic, strong) UIView *bottomControlView;
@property (nonatomic, strong) UIButton *backButton;
@property (nonatomic, strong) UILabel *titleLabel;
@property (nonatomic, strong) UIButton *snapshotButton;
@property (nonatomic, strong) UIButton *listButton;
@property (nonatomic, strong) UITextField *inputTextField;
@property (nonatomic, strong) UIButton *sendButton;
@property (nonatomic, strong) UITextView *channelTextView;
@property (nonatomic, strong) UIActivityIndicatorView *bufferingIndicate;
@property (nonatomic, strong) UILabel *bufferingReminder;

@end

@implementation LivePlayerViewController

CGFloat screenWidth;
CGFloat screenHeight;
float margin = 12.5;
float volumeLevel = 0.0f;

BOOL isStatusBarHide = NO;
BOOL isMute = NO;
BOOL isChannelHide = NO;

NSString *messageCallbackId;
CDVLivePlayer *cdvLivePLayer;

- (instancetype)initWithURL:(NSURL *)url title:(NSString *)title
{
  self = [self initWithNibName:nil bundle:nil];
  if (self) {
    self.url = url;
    self.streamingTitle = title;
  }
  return self;
}

- (void)willAnimateRotationToInterfaceOrientation:(UIInterfaceOrientation)toInterfaceOrientation duration:(NSTimeInterval)duration
{
  [self syncUIInterfaceOrientation:toInterfaceOrientation];
}

- (void)syncUIInterfaceOrientation:(UIInterfaceOrientation)toInterfaceOrientation
{
  screenWidth = CGRectGetWidth([UIScreen mainScreen].bounds);
  screenHeight = CGRectGetHeight([UIScreen mainScreen].bounds);

  self.streamingOverlay.frame = CGRectMake(0, 0, screenWidth, screenHeight);
  self.controlOverlay.frame = CGRectMake(0, 0, screenWidth, screenHeight);
  self.topControlView.frame = CGRectMake(0, 0, screenWidth, 64);
  self.bottomControlView.frame = CGRectMake(0, screenHeight - 44, screenWidth, 44);
  if (toInterfaceOrientation == UIInterfaceOrientationPortrait) {
    self.backButton.frame = CGRectMake(0, 20, 44, 44);
    self.snapshotButton.frame = CGRectMake(screenWidth - 44, 0, 44, 44);
  }
  else {
    self.backButton.frame = CGRectMake(margin, 20, 44, 44);
    self.snapshotButton.frame = CGRectMake(screenWidth - 44 - margin, 0, 44, 44);
  }
  self.titleLabel.frame = CGRectMake(0, 20, screenWidth, 44);

  self.playerView.frame = CGRectMake(0, 0, screenWidth, screenHeight);
  self.player.view.frame = self.playerView.bounds;
}

- (void)addChannelName:(NSString *)name andMessage:(NSString *)message
{
  NSMutableAttributedString *attributedText = [[NSMutableAttributedString alloc] initWithAttributedString:self.channelTextView.attributedText];
  NSMutableAttributedString *newAttributedText = [[NSMutableAttributedString alloc] initWithString:[NSString stringWithFormat:@"%@: %@\n", name, message] attributes:@{NSForegroundColorAttributeName: [UIColor whiteColor]}];
  [newAttributedText addAttribute:NSForegroundColorAttributeName value:UIColorFromRGB(0x28ECFD) range:NSMakeRange(0, name.length)];
  [attributedText appendAttributedString:newAttributedText];
  self.channelTextView.attributedText = attributedText;
  [self.channelTextView scrollRangeToVisible:NSMakeRange(0, self.channelTextView.attributedText.length - 1)];
}

- (void)setCommandDelegate:(CDVLivePlayer *)delegate andMessageCallbackId:(NSString *)callbackId
{
  cdvLivePLayer = delegate;
  messageCallbackId = callbackId;  NSLog(@"onClickSend called.");
  if (self.inputTextField.text.length > 0) {
    [cdvLivePLayer successWithCallbackId:messageCallbackId withMessage:self.inputTextField.text andKeep:YES];
    self.inputTextField.text = @"";
  }
  [self.inputTextField resignFirstResponder];
}

- (void)loadView
{
  // swapped for landscape orientation
  screenHeight = CGRectGetWidth([UIScreen mainScreen].bounds);
  screenWidth = CGRectGetHeight([UIScreen mainScreen].bounds);

  self.view = [[UIView alloc] initWithFrame:[[UIScreen mainScreen] applicationFrame]];
  self.playerView = [[UIView alloc] initWithFrame:CGRectMake(0, 0, screenWidth, screenHeight)];

  // streaming overlay
  self.streamingOverlay = [[UIControl alloc] initWithFrame:CGRectMake(0, 0, screenWidth, screenHeight)];
  [self.streamingOverlay addTarget:self action:@selector(onClickStreamingOverlay:) forControlEvents:UIControlEventTouchDown];

  // control overlay
  self.controlOverlay = [[UIControl alloc] initWithFrame:CGRectMake(0, 0, screenWidth, screenHeight)];
  [self.controlOverlay addTarget:self action:@selector(onClickControlOverlay:) forControlEvents:UIControlEventTouchDown];

  // top control view
  self.topControlView = [[UIView alloc] initWithFrame:CGRectMake(0, 0, screenWidth, 64)];
  self.topControlView.backgroundColor = [UIColor blackColor];
  self.topControlView.alpha = 0.7;

  // back button
  self.backButton = [UIButton buttonWithType:UIButtonTypeCustom];
  [self.backButton setTitle:@"完成" forState:UIControlStateNormal];
  self.backButton.frame = CGRectMake(margin, 20, 44, 44);
  [self.backButton addTarget:self action:@selector(onClickBack:) forControlEvents:UIControlEventTouchUpInside];
  [self.topControlView addSubview:self.backButton];

  // title label
  self.titleLabel = [[UILabel alloc] initWithFrame:CGRectMake(0, 20, screenWidth, 44)];
  self.titleLabel.text = self.streamingTitle;
  self.titleLabel.textAlignment = NSTextAlignmentCenter;
  self.titleLabel.textColor = [UIColor whiteColor];
  [self.topControlView addSubview:self.titleLabel];

  // channel text view
  self.channelTextView = [[UITextView alloc] initWithFrame:CGRectMake(screenWidth - 140 - 7.5, 64 + 7.5, 140, screenHeight - 64 - 44 - 15)];
  self.channelTextView.returnKeyType = UIReturnKeyDone;
  self.channelTextView.backgroundColor = [UIColor clearColor];
  self.channelTextView.font = [UIFont systemFontOfSize:15];
  self.channelTextView.textColor = [UIColor whiteColor];
  self.channelTextView.textContainer.lineBreakMode = NSLineBreakByWordWrapping;
  self.channelTextView.selectable = NO;

  // bottom control view
  self.bottomControlView = [[UIView alloc] initWithFrame:CGRectMake(0, screenHeight - 44, screenWidth, 44)];
  self.bottomControlView.backgroundColor = [UIColor blackColor];
  self.bottomControlView.alpha = 0.7;

  // input textfield
  self.inputTextField = [[UITextField alloc] initWithFrame:CGRectMake(50, 10, 350, 24)];
  self.inputTextField.backgroundColor = [UIColor whiteColor];
  self.inputTextField.borderStyle = UITextBorderStyleRoundedRect;
  self.inputTextField.layer.cornerRadius = 4.0f;
  self.inputTextField.font = [UIFont systemFontOfSize:13];
  self.inputTextField.placeholder = @"输入你想说的话和你想问的";
  self.inputTextField.autocorrectionType = UITextAutocorrectionTypeNo;
  self.inputTextField.keyboardType = UIKeyboardTypeDefault;
  self.inputTextField.returnKeyType = UIReturnKeyDone;
  self.inputTextField.clearButtonMode = UITextFieldViewModeWhileEditing;
  self.inputTextField.contentVerticalAlignment = UIControlContentVerticalAlignmentCenter;
  [self.bottomControlView addSubview:self.inputTextField];

  // send button
  self.sendButton = [UIButton buttonWithType:UIButtonTypeRoundedRect];
  self.sendButton.layer.cornerRadius = 4.0f;
  self.sendButton.frame = CGRectMake(0, 0, 65, 24);
  self.sendButton.backgroundColor = UIColorFromRGB(0xDF00A5);
  self.sendButton.titleLabel.font = [UIFont systemFontOfSize:13];
  self.sendButton.titleLabel.textColor = [UIColor whiteColor];
  [self.sendButton setTitle:@"发送" forState:UIControlStateNormal];
  [self.sendButton addTarget:self action:@selector(onClickSend:) forControlEvents:UIControlEventTouchUpInside];
  self.inputTextField.rightView = self.sendButton;
  self.inputTextField.rightViewMode = UITextFieldViewModeAlways;

  // snapshot button
  self.snapshotButton = [UIButton buttonWithType:UIButtonTypeCustom];
  [self.snapshotButton setImage:[UIImage imageNamed:@"CDVLivePlayer.bundle/snapshot"] forState:UIControlStateNormal];
  self.snapshotButton.frame = CGRectMake(screenWidth - 44 - margin, 0, 44, 44);
  [self.snapshotButton addTarget:self action:@selector(onClickSnapshot:) forControlEvents:UIControlEventTouchUpInside];
  [self.bottomControlView addSubview:self.snapshotButton];

  // list button
  self.listButton = [UIButton buttonWithType:UIButtonTypeCustom];
  [self.listButton setImage:[UIImage imageNamed:@"CDVLivePlayer.bundle/list"] forState:UIControlStateNormal];
  self.listButton.frame = CGRectMake(screenWidth - 44 * 2 - margin * 2, 0, 44, 44);
  [self.listButton addTarget:self action:@selector(onClickList:) forControlEvents:UIControlEventTouchUpInside];
  [self.bottomControlView addSubview:self.listButton];

  [self.controlOverlay addSubview:self.topControlView];
  [self.controlOverlay addSubview:self.bottomControlView];
  [self.controlOverlay addSubview:self.channelTextView];
  [self.streamingOverlay addSubview:self.controlOverlay];

  self.player = [[NELivePlayerController alloc] initWithContentURL:self.url];
  self.player.view.frame = self.playerView.bounds;

  // buffering
  self.bufferingIndicate = [[UIActivityIndicatorView alloc] initWithFrame:CGRectMake(0, 0, 30, 30)];
  [self.bufferingIndicate setCenter:CGPointMake(screenWidth/2, screenHeight/2 - 50)];
  [self.bufferingIndicate setActivityIndicatorViewStyle:UIActivityIndicatorViewStyleWhiteLarge];
  self.bufferingIndicate.hidden = YES;

  self.bufferingReminder = [[UILabel alloc] initWithFrame:CGRectMake(0, 0, 200, 30)];
  [self.bufferingReminder setCenter:CGPointMake(screenWidth/2, screenHeight/2)];
  self.bufferingReminder.text = @"正在缓冲中，请稍后…";
  self.bufferingReminder.textAlignment = NSTextAlignmentCenter;
  self.bufferingReminder.textColor = UIColorFromRGB(0x727376);
  self.bufferingReminder.font = [UIFont fontWithName:self.bufferingReminder.font.fontName size:13.0];
  self.bufferingReminder.hidden = YES;

  [self.view addSubview:self.player.view];
  [self.view addSubview:self.streamingOverlay];
  [self.view addSubview:self.bufferingIndicate];
  [self.view addSubview:self.bufferingReminder];
}

- (void)viewWillAppear:(BOOL)animated {
  [super viewWillAppear:animated];
  [self.player setBufferStrategy:NELPLowDelay];
  [self.player setScalingMode:NELPMovieScalingModeAspectFit];
  [self.player setShouldAutoplay:YES];
  [self.player setPauseInBackground:NO];
  [self.player setHardwareDecoder:NO];
  [self.player prepareToPlay];

  NSNumber *value = [NSNumber numberWithInt:UIInterfaceOrientationLandscapeRight];
  [[UIDevice currentDevice] setValue:value forKey:@"orientation"];
}

- (void)viewDidDisappear:(BOOL)animated {
  [super viewDidDisappear:animated];
  [self.player shutdown];

  // remove notifications
  [[NSNotificationCenter defaultCenter] removeObserver:self name:NELivePlayerDidPreparedToPlayNotification object:_player];
  [[NSNotificationCenter defaultCenter] removeObserver:self name:NELivePlayerLoadStateChangedNotification object:_player];
  [[NSNotificationCenter defaultCenter] removeObserver:self name:NELivePlayerPlaybackFinishedNotification object:_player];
  [[NSNotificationCenter defaultCenter] removeObserver:self name:NELivePlayerFirstVideoDisplayedNotification object:_player];
  [[NSNotificationCenter defaultCenter] removeObserver:self name:NELivePlayerReleaseSueecssNotification object:_player];
}

- (void)viewDidLoad {
  [super viewDidLoad];
  // add notifications
  [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(didPreparedToPlay:) name:NELivePlayerDidPreparedToPlayNotification object:_player];
  [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(loadStateChanged:) name:NELivePlayerLoadStateChangedNotification object:_player];
  [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(playBackFinished:) name:NELivePlayerPlaybackFinishedNotification object:_player];
  [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(firstVideoDisplayed:) name:NELivePlayerFirstVideoDisplayedNotification object:_player];
  [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(releaseSuccess:) name:NELivePlayerReleaseSueecssNotification object:_player];
  [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(keyboardWillShow:) name:UIKeyboardWillShowNotification object:nil];
  [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(keyboardWillHide:) name:UIKeyboardWillHideNotification object:nil];
  [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(keyboardWillShow:) name:UIKeyboardWillChangeFrameNotification object:nil];
}

- (BOOL)prefersStatusBarHidden
{
  return isStatusBarHide;
}

- (UIStatusBarStyle)preferredStatusBarStyle
{
  return UIStatusBarStyleLightContent;
}

- (UIInterfaceOrientationMask)supportedInterfaceOrientations
{
  return UIInterfaceOrientationMaskLandscape;
}

#pragma mark - IBActions


-(void)keyboardWillShow:(NSNotification *)notification
{
  NSDictionary *keyboardInfo = [notification userInfo];
  CGSize keyboardSize = [[keyboardInfo objectForKey:UIKeyboardFrameEndUserInfoKey] CGRectValue].size;
  self.bottomControlView.frame = CGRectMake(0, screenHeight - 44 - keyboardSize.height, screenWidth, 44);
}

-(void)keyboardWillHide:(NSNotification *)notification
{
  self.bottomControlView.frame = CGRectMake(0, screenHeight - 44, screenWidth, 44);
}

- (void)onClickStreamingOverlay:(id)sender
{
  NSLog(@"onClickStreamingOverlay called.");
  self.controlOverlay.hidden = NO;
  isStatusBarHide = NO;
  [self setNeedsStatusBarAppearanceUpdate];
}

- (void)onClickControlOverlay:(id)sender
{
  NSLog(@"onClickControlOverlay called.");
  self.controlOverlay.hidden = YES;
  isStatusBarHide = YES;
  [self setNeedsStatusBarAppearanceUpdate];
}

- (void)onClickBack:(id)sender
{
  NSLog(@"onClickBack called.");
  if (self.presentingViewController)
  {
    [self.presentingViewController dismissViewControllerAnimated:NO completion:nil];
  }
}

- (void)onClickSend:(id)sender
{
  NSLog(@"onClickSend called.");
  if (self.inputTextField.text.length > 0) {
    [cdvLivePLayer successWithCallbackId:messageCallbackId withMessage:self.inputTextField.text andKeep:YES];
    self.inputTextField.text = @"";
  }
  [self.inputTextField resignFirstResponder];
}

- (void)onClickList:(id)sender
{
  NSLog(@"onClickList called.");
  isChannelHide = !isChannelHide;
  self.channelTextView.hidden = isChannelHide;
}

- (void)onClickSnapshot:(id)sender
{
  NSLog(@"onClickSnapshot called.");
  UIImage *snapImage = [self.player getSnapshot];
  UIImageWriteToSavedPhotosAlbum(snapImage, nil, nil, nil);

  PHAuthorizationStatus status = [PHPhotoLibrary authorizationStatus];
  BOOL authorized = NO;
  switch (status) {
    case PHAuthorizationStatusDenied:
    case PHAuthorizationStatusRestricted:
    case PHAuthorizationStatusNotDetermined:
      authorized = NO;
      break;
    case PHAuthorizationStatusAuthorized:
      authorized = YES;
      break;
  }

  UIAlertController *alertController = NULL;
  if (authorized) {
    alertController = [UIAlertController alertControllerWithTitle:@"截图已保存到相册" message:nil preferredStyle:UIAlertControllerStyleAlert];
  }
  else {
    alertController = [UIAlertController alertControllerWithTitle:@"无法访问相册" message:@"请在设置中允许悠课访问你的相册" preferredStyle:UIAlertControllerStyleAlert];
  }
  UIAlertAction *action = [UIAlertAction actionWithTitle:@"确定" style:UIAlertActionStyleDefault handler:^(UIAlertAction *action){}];
  [alertController addAction:action];
  [self presentViewController:alertController animated:YES completion:nil];
}

#pragma mark - Notification Handlers
- (void)didPreparedToPlay:(NSNotification *)notification
{
  NSLog(@"didPreparedToPlay called.");
  [self.player play];
}

- (void)loadStateChanged:(NSNotification *)notification
{
  NSLog(@"loadStateChanged called.");
  NELPMovieLoadState nelpLoadState = self.player.loadState;

  if (nelpLoadState == NELPMovieLoadStatePlaythroughOK) {
    self.bufferingIndicate.hidden = YES;
    self.bufferingReminder.hidden = YES;
    [self.bufferingIndicate stopAnimating];
  }
  else if (nelpLoadState == NELPMovieLoadStateStalled) {
    self.bufferingIndicate.hidden = NO;
    self.bufferingReminder.hidden = NO;
    [self.bufferingIndicate startAnimating];
  }
}

- (void)playBackFinished:(NSNotification *)notification
{
  NSLog(@"playBackFinished called.");
  UIAlertController *alertController = NULL;
  UIAlertAction *action = [UIAlertAction actionWithTitle:@"确定" style:UIAlertActionStyleDefault handler:^(UIAlertAction *action) {
    if (self.presentingViewController) {
      [self.presentingViewController dismissViewControllerAnimated:NO completion:nil];
    }
  }];
  switch ([[[notification userInfo] valueForKey:NELivePlayerPlaybackDidFinishReasonUserInfoKey] intValue]) {
    case NELPMovieFinishReasonPlaybackEnded:
      alertController = [UIAlertController alertControllerWithTitle:@"直播已结束" message:nil preferredStyle:UIAlertControllerStyleAlert];
      break;
    case NELPMovieFinishReasonPlaybackError:
      alertController = [UIAlertController alertControllerWithTitle:@"直播已结束" message:nil preferredStyle:UIAlertControllerStyleAlert];
      break;
    case NELPMovieFinishReasonUserExited:
      break;
    default:
      break;
  }
  [alertController addAction:action];
  [self presentViewController:alertController animated:YES completion:nil];
}

- (void)firstVideoDisplayed:(NSNotification *)notification
{
  NSLog(@"firstVideoDisplayed called.");
}

- (void)releaseSuccess:(NSNotification *)notification
{
  NSLog(@"releaseSuccess called.");
}

@end