//
//  hkbusetaApp.swift
//  hkbuseta Watch App
//
//  Created by LOOHP on 19/12/2023.
//

import SwiftUI
import shared

@main
struct hkbuseta_Watch_AppApp: App {
    
    @StateObject private var historyStackState = FlowStateObservable(defaultValue: appContext().historyStack, nativeFlow: appContext().historyStackFlow)
    
    var body: some Scene {
        WindowGroup {
            let item = historyStackState.state.last!
            ZStack {
                switch item.screen {
                case AppScreen.dummy:
                    DummyView(data: item.data, storage: item.storage).defaultStyle()
                case AppScreen.main:
                    MainView(data: item.data, storage: item.storage).defaultStyle()
                case AppScreen.title:
                    TitleView(data: item.data, storage: item.storage).defaultStyle()
                case AppScreen.search:
                    SearchView(data: item.data, storage: item.storage).defaultStyle()
                case AppScreen.nearby:
                    NearbyView(data: item.data, storage: item.storage).defaultStyle()
                case AppScreen.listRoutes:
                    ListRoutesView(data: item.data, storage: item.storage).defaultStyle()
                case AppScreen.listStops:
                    ListStopsView(data: item.data, storage: item.storage).defaultStyle()
                case AppScreen.eta:
                    EtaView(data: item.data, storage: item.storage).defaultStyle()
                case AppScreen.etaMenu:
                    EtaMenuView(data: item.data, storage: item.storage).defaultStyle()
                case AppScreen.fav:
                    FavView(data: item.data, storage: item.storage).defaultStyle()
                case AppScreen.favRouteListView:
                    FavRouteListViewView(data: item.data, storage: item.storage).defaultStyle()
                default:
                    MainView(data: item.data, storage: item.storage).defaultStyle()
                }
                if item.screen.needBackButton() {
                    BackButton(scrollingScreen: item.screen.isScrollingScreen())
                }
            }
        }
    }
}

func BackButton(scrollingScreen: Bool) -> some View {
    ZStack {
        Button(action: {
            appContext().popStack()
        }) {
            Image(systemName: "arrow.left")
                .font(.system(size: 17.scaled()))
                .bold()
                .foregroundColor(.white)
        }
        .frame(width: 30.scaled(), height: 30.scaled())
        .buttonStyle(PlainButtonStyle())
        .position(x: 23.scaled(), y: 23.scaled())
    }
    .background(alignment: .top) {
        if scrollingScreen {
            LinearGradient(gradient: Gradient(colors: [0xFF000000.asColor(), 0xFF000000.asColor(), 0xFF000000.asColor(), 0x00000000.asColor()]), startPoint: .top, endPoint: .bottom)
                .frame(height: 45.scaled())
        }
    }
    .frame(
        maxWidth: .infinity,
        maxHeight: .infinity,
        alignment: .top
    )
    .edgesIgnoringSafeArea(.all)
}

extension View {
    
    func defaultStyle() -> some View {
        return self
            .transition(AnyTransition.scale.animation(.easeInOut(duration: 0.25)))
            .background { 0xFF000000.asColor() }
    }
    
}

extension AppScreen {
    
    func needBackButton() -> Bool {
        switch self {
        case AppScreen.main, AppScreen.title:
            return false
        default:
            return true
        }
    }
    
    func isScrollingScreen() -> Bool {
        switch self {
        case AppScreen.listStops, AppScreen.listRoutes, AppScreen.fav:
            return true
        default:
            return false
        }
    }
    
}
