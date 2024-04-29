
#import "RNBoundary.h"

@implementation RNBoundary

RCT_EXPORT_MODULE()

-(instancetype)init
{
    self = [super init];
    if (self) {
        self.locationManager = [[CLLocationManager alloc] init];
        self.locationManager.delegate = self;
        self.hasListeners = NO;
    }

    return self;
}

// Will be called when this module's first listener is added.
-(void)startObserving {
    self.hasListeners = YES;
    // Set up any upstream listeners or background tasks as necessary
}

// Will be called when this module's last listener is removed, or on dealloc.
-(void)stopObserving {
    self.hasListeners = NO;
    // Remove upstream listeners, stop unnecessary background tasks
}

RCT_EXPORT_METHOD(add:(NSDictionary*)boundary addWithResolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject)
{
    if (CLLocationManager.authorizationStatus != kCLAuthorizationStatusAuthorizedAlways) {
        [self.locationManager requestAlwaysAuthorization];
    }

    if ([CLLocationManager authorizationStatus] == kCLAuthorizationStatusAuthorizedAlways) {
        NSString *id = boundary[@"id"];
        CLLocationCoordinate2D center = CLLocationCoordinate2DMake([boundary[@"lat"] doubleValue], [boundary[@"lng"] doubleValue]);
        CLRegion *boundaryRegion = [[CLCircularRegion alloc]initWithCenter:center
                                                                    radius:[boundary[@"radius"] doubleValue]
                                                                identifier:id];

        [self.locationManager startMonitoringForRegion:boundaryRegion];

        resolve(id);
    } else {
        reject(@"PERM", @"Access fine location is not permitted", [NSError errorWithDomain:@"boundary" code:200 userInfo:@{@"Error reason": @"Invalid permissions"}]);
    }
}
RCT_EXPORT_METHOD(addList:(NSArray*)boundaries addWithResolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject)
{
    if (CLLocationManager.authorizationStatus != kCLAuthorizationStatusAuthorizedAlways) {
        [self.locationManager requestAlwaysAuthorization];
    }

    if ([CLLocationManager authorizationStatus] == kCLAuthorizationStatusAuthorizedAlways) {
        NSMutableArray *boundaryIds;
        boundaryIds = [[NSMutableArray alloc] init];

        for (NSDictionary *boundary in boundaries) {
            NSString *id = boundary[@"id"];

            [boundaryIds addObject:id];

            CLLocationCoordinate2D center = CLLocationCoordinate2DMake([boundary[@"lat"] doubleValue], [boundary[@"lng"] doubleValue]);
            CLRegion *boundaryRegion = [[CLCircularRegion alloc]initWithCenter:center
                                                                    radius:[boundary[@"radius"] doubleValue]
                                                                identifier:id];

            [self.locationManager startMonitoringForRegion:boundaryRegion];
        }
        resolve(boundaryIds);
    } else {
        reject(@"PERM", @"Access fine location is not permitted", [NSError errorWithDomain:@"boundary" code:200 userInfo:@{@"Error reason": @"Invalid permissions"}]);
    }
}

RCT_EXPORT_METHOD(remove:(NSString *)boundaryId removeWithResolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject)
{
    if ([self removeBoundary:boundaryId]) {
        resolve(boundaryId);
    } else {
        reject(@"@no_boundary", @"No boundary with the provided id was found", [NSError errorWithDomain:@"boundary" code:200 userInfo:@{@"Error reason": @"Invalid boundary ID"}]);
    }
}

RCT_EXPORT_METHOD(removeList:(NSArray *)boundaryIds removeWithResolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject)
{
    NSMutableArray *removedIds;
    removedIds = [[NSMutableArray alloc] init];

    for(NSString *boundaryId in boundaryIds) {
        if ([self removeBoundary:boundaryId]) {
            [removedIds addObject:boundaryId];
        }
    }
    resolve(removedIds);
}

RCT_EXPORT_METHOD(removeAll:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject)
{
    @try {
        [self removeAllBoundaries];
    }
    @catch (NSError *ex) {
        reject(@"failed_remove_all", @"Failed to remove all boundaries", ex);
    }
    resolve(NULL);
}

- (void) removeAllBoundaries
{
    for(CLRegion *region in [self.locationManager monitoredRegions]) {
        [self.locationManager stopMonitoringForRegion:region];
    }
}

- (bool) removeBoundary:(NSString *)boundaryId
{
    for(CLRegion *region in [self.locationManager monitoredRegions]){
        if ([region.identifier isEqualToString:boundaryId]) {
            [self.locationManager stopMonitoringForRegion:region];
            return true;
        }
    }
    return false;
}

- (NSArray<NSString *> *)supportedEvents
{
    return @[@"onEnter", @"onExit"];
}

- (void)locationManager:(CLLocationManager *)manager didEnterRegion:(CLRegion *)region
{
    NSLog(@"didEnter : %@", region);
    if (self.hasListeners) {// Only send events if anyone is listening
        [self sendEventWithName:@"onEnter" body:region.identifier];
    }
    
}

- (void)locationManager:(CLLocationManager *)manager didExitRegion:(CLRegion *)region
{
    NSLog(@"didExit : %@", region);
    if (self.hasListeners) {// Only send events if anyone is listening
        [self sendEventWithName:@"onExit" body:region.identifier];
    }
}

+ (BOOL)requiresMainQueueSetup
{
  return YES;
}

@end
