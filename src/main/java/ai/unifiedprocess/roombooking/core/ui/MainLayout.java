package ai.unifiedprocess.roombooking.core.ui;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.router.Layout;
import com.vaadin.flow.server.menu.MenuConfiguration;

/**
 * Application shell around every view. The main menu lists every view annotated with {@code @Menu}, so a new view
 * appears in the menu without a change here.
 */
@Layout
public class MainLayout extends AppLayout {

    /** Accessible name of the drawer's navigation landmark. */
    public static final String NAV_LABEL = "Main menu";

    public MainLayout() {
        H1 title = new H1("Room Booking");
        title.getStyle().set("font-size", "var(--aura-font-size-l, 1.25rem)").set("margin", "0");

        HorizontalLayout header = new HorizontalLayout(new DrawerToggle(), title);
        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        header.setWidthFull();
        addToNavbar(header);

        SideNav nav = new SideNav();
        nav.getElement().setAttribute("aria-label", NAV_LABEL);
        MenuConfiguration.getMenuEntries()
                .forEach(entry -> nav.addItem(new SideNavItem(entry.title(), entry.path())));
        addToDrawer(nav);
    }
}
