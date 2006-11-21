/**********************************************************************************
 * $URL: https://source.sakaiproject.org/svn/msgcntr/trunk/messageforums-app/src/java/org/sakaiproject/tool/messageforums/ui/MessageForumsSynopticBean.java $
 * $Id: MessageForumsSynopticBean.java $
 ***********************************************************************************
 *
 * Copyright (c) 2003, 2004, 2005, 2006 The Sakai Foundation.
 * 
 * Licensed under the Educational Community License, Version 1.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at
 * 
 *      http://www.opensource.org/licenses/ecl1.php
 * 
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 *
 **********************************************************************************/
package org.sakaiproject.tool.messageforums.ui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.faces.event.ActionEvent;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sakaiproject.api.app.messageforums.Area;
import org.sakaiproject.api.app.messageforums.AreaManager;
import org.sakaiproject.api.app.messageforums.DiscussionForum;
import org.sakaiproject.api.app.messageforums.DiscussionTopic;
import org.sakaiproject.api.app.messageforums.MessageForumsMessageManager;
import org.sakaiproject.api.app.messageforums.MessageForumsTypeManager;
import org.sakaiproject.api.app.messageforums.PrivateForum;
import org.sakaiproject.api.app.messageforums.PrivateMessage;
import org.sakaiproject.api.app.messageforums.Topic;
import org.sakaiproject.api.app.messageforums.ui.DiscussionForumManager;
import org.sakaiproject.api.app.messageforums.ui.PrivateMessageManager;
import org.sakaiproject.api.app.messageforums.ui.UIPermissionsManager;
import org.sakaiproject.authz.cover.AuthzGroupService;
import org.sakaiproject.entity.api.ResourceProperties;
import org.sakaiproject.entity.api.ResourcePropertiesEdit;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.tool.cover.SessionManager;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.site.api.SitePage;
import org.sakaiproject.site.api.ToolConfiguration;
import org.sakaiproject.site.cover.SiteService;
import org.sakaiproject.tool.cover.ToolManager;
import org.sakaiproject.user.api.Preferences;
import org.sakaiproject.user.api.PreferencesEdit;
import org.sakaiproject.user.api.PreferencesService;
import org.sakaiproject.util.ResourceLoader;

public class MessageForumSynopticBean {

	/**
	 * Used to store synoptic information for a users unread messages.
	 * Whether on the Home page of a site or in MyWorkspace determines
	 * what properties are filled.
	 * <p>
	 * If in MyWorkspace, each object contains the number of unread
	 * Private Messages and number of unread Discussion Forum messages.
	 * </p>
	 * <p>
	 * If in the Home page of a site, each object contains either the
	 * number of unread Private Messages or number of unread Discussion 
	 * Forum messages.</p>
	 * 
	 * @author josephrodriguez
	 *
	 */
	public class DecoratedCompiledMessageStats {
		private String siteName;
		private String siteId;
		
		/** MyWorkspace information */
		private int unreadPrivateAmt;
		private int unreadForumsAmt;
		private String mcPageURL;
		
		/** site Home page information */
		private int unreadMessages;
		private String heading;

		public String getHeading() {
			return heading;
		}

		public void setHeading(String heading) {
			this.heading = heading;
		}

		/**
		 * 
		 * @return
		 */
		public String getSiteName() {
			return siteName;
		}

		/**
		 * 
		 * @param siteName
		 */
		public void setSiteName(String siteName) {
			this.siteName = siteName;
		}

		/**
		 * 
		 * @return
		 */
		public int getUnreadPrivateAmt() {
			return unreadPrivateAmt;
		}

		/**
		 * 
		 * @param unreadPrivateAmt
		 */
		public void setUnreadPrivateAmt(int unreadPrivateAmt) {
			this.unreadPrivateAmt = unreadPrivateAmt;
		}

		/**
		 * 
		 * @return
		 */
		public int getUnreadForumsAmt() {
			return unreadForumsAmt;
		}

		/**
		 * 
		 * @param unreadForumsAmt
		 */
		public void setUnreadForumsAmt(int unreadForumsAmt) {
			this.unreadForumsAmt = unreadForumsAmt;
		}

		/**
		 * 
		 * @return
		 */
		public String getMcPageURL() {
			return mcPageURL;
		}

		/**
		 * 
		 * @param mcPageURL
		 */
		public void setMcPageURL(String mcPageURL) {
			this.mcPageURL = mcPageURL;
		}

		/**
		 * 
		 * @return
		 */
		public String getSiteId() {
			return siteId;
		}

		/**
		 * 
		 * @param siteId
		 */
		public void setSiteId(String siteId) {
			this.siteId = siteId;
		}

		/**
		 * 
		 * @return
		 */
		public int getUnreadMessages() {
			return unreadMessages;
		}

		/**
		 * 
		 * @param unreadMessages
		 */
		public void setUnreadMessages(int unreadMessages) {
			this.unreadMessages = unreadMessages;
		}

	}

/* =========== End of DecoratedCompiledMessageStats =========== */

	/** Used to display 'Private Messages' on tool when in home page of site */
	private final String PRIVATE_HEADING = "syn_private_heading";
	
	/** Use to display 'Discussion Forums' on tool when in home page of site */
	private final String DISCUSSION_HEADING = "syn_discussion_heading";
	
	/** Used to determine if MessageCenter tool part of site */
	private final String MESSAGE_CENTER_ID = "sakai.messagecenter";

	/** Used to get contextId when tool on MyWorkspace to set all private messages to Read status */
	private final String CONTEXTID="contextId";

	/** =============== Options page bean values =============== */

	/** Used to retrieve non-notification sites for MyWorkspace page */
	private final String SYNMC_OPTIONS_PREFS = "synmc_hidden_sites";
	
	/** Used to pull excluded sites from user preferences */
	private final String EXCLUDE_STRING = "excludeSiteMC";
	
	/**
	 * Taken from UserPrefsTool. Sites removed from synoptic Message Center
	 * on MyWorkspace are saved in user preferences. This is the class
	 * user-prefs-tool grabs/stores the properties in user preferences
	 * so doing the same.
	 */
	public class KeyNameValue
	{
		/** Is this value a list?. */
		protected boolean m_isList = false;

		/** The key. */
		protected String m_key = null;

		/** The name. */
		protected String m_name = null;

		/** The original is this value a list?. */
		protected boolean m_origIsList = false;

		/** The original key. */
		protected String m_origKey = null;

		/** The original name. */
		protected String m_origName = null;

		/** The original value. */
		protected String m_origValue = null;

		/** The value. */
		protected String m_value = null;

	public KeyNameValue(String key, String name, String value, boolean isList)
	{
		m_key = key;
		m_origKey = key;
		m_name = name;
		m_origName = name;
		m_value = value;
		m_origValue = value;
		m_isList = isList;
		m_origIsList = isList;
	}

	public String getKey()
	{
		return m_key;
	}

	public String getName()
	{
		return m_name;
	}

	public String getOrigKey()
	{
		return m_origKey;
	}

	public String getOrigName()
	{
		return m_origName;
	}

	public String getOrigValue()
	{
		return m_origValue;
	}

	public String getValue()
	{
		return m_value;
	}

	public boolean isChanged()
	{
		return ((!m_name.equals(m_origName)) || (!m_value.equals(m_origValue)) || (!m_key.equals(m_origKey)) || (m_isList != m_origIsList));
	}

	public boolean isList()
	{
		return m_isList;
	}

	public boolean origIsList()
	{
		return m_origIsList;
	}

	public void setKey(String value)
	{
		if (!m_key.equals(value))
		{
			m_key = value;
		}
	}

	public void setList(boolean b)
	{
		m_isList = b;
	}

	public void setName(String value)
	{
		if (!m_name.equals(value))
		{
			m_name = value;
		}
	}

	public void setValue(String value)
	{
		if (!m_value.equals(value))
		{
			m_value = value;
		}
	}
}

	/** ============ End KeyNameValue class ========== */

	/** Used to store list of sites that will be displayed
	 * as well as sites to exclude within the Options page.
	 * The List is for the information coming from user 
	 * preferences and the String [] are to display in the 
	 * multi-item select boxes on the page.
	 */
	private List notificationSitesItems = new ArrayList();
	private String [] notificationSites;
	private List nonNotificationSitesItems = new ArrayList();
	private String [] nonNotificationSites;

	/** The PreferencesEdit being worked on. */
	protected PreferencesEdit prefsEdit = null;

	/** Preferences service (injected dependency) */
	protected PreferencesService preferencesService = null;

	/** The PreferencesEdit in KeyNameValue collection form. */
	protected Collection prefsEditKNVCollection = null;

	/** =============== End Options page bean values =============== */

	/** Used to determine if there are sites to display on page */
	private boolean sitesToView;
	private boolean sitesToViewSet = false;
	
	/** Resource loader to grab bundle messages */
	private static ResourceLoader rb = new ResourceLoader("org.sakaiproject.tool.messageforums.bundle.Messages");
	
	/** Used to display no site selected error message on Options page */
	private final String NO_SITE_SELECTED_MSG = "syn_no_site_selected";

	/** to get accces to log file */
	private static final Log LOG = LogFactory.getLog(MessageForumSynopticBean.class);

	/** Needed if within a site so we only need stats for this site */
	private MessageForumsMessageManager messageManager;

	/** Needed to get topics if tool within a site */
	private DiscussionForumManager forumManager;

	/** Needed to grab unread message count if tool within site */
	private PrivateMessageManager pvtMessageManager;

	/** Needed to get forum message counts as well as Uuids for private messages and discussions */
	private MessageForumsTypeManager typeManager;

	/** Needed to set up the counts for the private messages and forums */
	private AreaManager areaManager;
	
	/** Needed to determine if user has read permission of topic */
	private UIPermissionsManager uiPermissionsManager;
	
	/**
	 * 
	 * @param messageManager
	 */
	public void setMessageManager(MessageForumsMessageManager messageManager) {
		this.messageManager = messageManager;
	}

	/**
	 * 
	 * @param forumManager
	 */
	public void setForumManager(DiscussionForumManager forumManager) {
		this.forumManager = forumManager;
	}

	/**
	 * 
	 * @param pvtMessageManager
	 */
	public void setPvtMessageManager(PrivateMessageManager pvtMessageManager) {
		this.pvtMessageManager = pvtMessageManager;
	}

	/**
	 * 
	 * @param typeManager
	 */
	public void setTypeManager(MessageForumsTypeManager typeManager) {
		this.typeManager = typeManager;
	}

	/**
	 * 
	 * @param areaManager
	 */
	public void setAreaManager(AreaManager areaManager) {
		this.areaManager = areaManager;
	}

	/**
	 * 
	 * @param uiPermissionsManager
	 */
	public void setUiPermissionsManager(UIPermissionsManager uiPermissionsManager) {
		this.uiPermissionsManager = uiPermissionsManager;
	}

	/**
	 * 
	 * @param preferencesService
	 */
	public void setPreferencesService(PreferencesService preferencesService) {
		this.preferencesService = preferencesService;
	}

	/**
	 * Returns TRUE if on MyWorkspace, FALSE if on a specific site
	 * 
	 * @return
	 * 		TRUE if on MyWorkspace, FALSE if on a specific site
	 */
	public boolean isMyWorkspace() {

		// get Site id
		final String siteId = getContext();

		if (SiteService.getUserSiteId("admin").equals(siteId))
			return false;

		final boolean where = SiteService.isUserSite(siteId);

		LOG.debug("Result of determinig if My Workspace: " + where);

		return where;
	}

	/**
	 * Returns TRUE if there is at least one site user can access
	 * and user has not set it to be excluded
	 *  
	 * @return
	 * 			TRUE if there are/is site(s) user can access
	 * 			FALSE if not
	 */
	public boolean isSitesToView() {
		if (sitesToViewSet) {
			return sitesToView;
		}
		else {
			return ! filterOutExcludedSites(getSiteList()).isEmpty();
		}
	}

	/**
	 * Removes from message counts messages that the user currently
	 * does not have read access to.
	 * 
	 * @param currentList
	 * 				List of message counts
	 * 
	 * @param removeList
	 * 				List of messages that user actually does not have
	 * 				read access to
	 * @return
	 * 		List with the adjusted message counts
	 */
	private List filterNoAccessMessages(List currentList, List removeList) {
		List resultList = new ArrayList();
		
		// ****** if either list is empty, return currentList unchanged ******
		if (currentList.isEmpty() || removeList.isEmpty()) {
			return currentList;
		}

		// ****** Set up our iterator ******
		final Iterator currentIter = currentList.iterator();
		
		while (currentIter.hasNext()) {
			final Object [] resultValues = new Object [2];
			Object [] removeValues;
			Object [] currentValues = null;
			
			// get current values for this iteration
			if (currentIter.hasNext()) {
				currentValues = (Object []) currentIter.next();
			}

			int pos = indexOf((String) currentValues[0], getSiteIds(removeList));
			
			// if there are messages to remove, do so otherwise just add current values
			if (pos != -1) {
				removeValues = (Object []) removeList.get(pos);

				resultValues[0] = currentValues[0];
				resultValues[1] = new Integer( ((Integer) currentValues[1]).intValue() - 
													((Integer) removeValues[2]).intValue() );
				
				resultList.add(resultValues);
				
				removeList.remove(pos);
			} 
			else {
				resultList.add(currentValues);
			}
		}
		
		return resultList;
	}

	/**
	 * Returns a List of all roles a user has for all sites
	 * they are a member of
	 * 
	 * @param siteList
	 * 				The List of site ids the user is a member of
	 * 
	 * @return
	 * 		List of role ids user has for all sites passed in
	 */
	private List getUserRoles(List siteList) {
		List roles = new ArrayList();
		
		final Iterator siteIter = siteList.iterator();
		
		while (siteIter.hasNext()) {
			
			Site curSite = null;
			
			try {
				curSite = getSite((String) siteIter.next());
			}
			catch (IdUnusedException e) {
				// Mucho weirdness, found by getSites() but now cannot find
				LOG.error("IdUnusedException will accessing site to determine user role");
			}
			
			if (curSite != null) {
				final String curRole = AuthzGroupService.getUserRole(SessionManager.getCurrentSessionUserId(), "/site/" + curSite.getId());
			
				if (curRole != null && ! roles.contains(curRole)) {
					roles.add(curRole);
				}
			}
		}
		
		return roles;
	}

	/**
	 * For this particular site, pick the correct role's count
	 * that needs to be removed from the total count
	 * 
	 * @param removeMessageCounts
	 * 				List of counts to be removed ordered by site id
	 * 
	 * @param siteList
	 * 				List of sites this user is a member of
	 * 
	 * @return
	 * 			List of correct counts, at most one per site
	 */
	private List selectCorrectRemoveMessageCount(List removeMessageCounts, List siteList) {
		// if message counts empty, nothing to do so return
		if (removeMessageCounts.isEmpty()) {
			return removeMessageCounts;
		}
		
		Object [] resultSet = null;		
		final List resultList = new ArrayList();
		
		// ***** set up iterators *****
		final Iterator siteIter = siteList.iterator();
		final Iterator rmIter = removeMessageCounts.iterator();
		
		while (siteIter.hasNext()) {
			
			Site site;
			
			try {
				site = getSite((String) siteIter.next());
			}
			catch (IdUnusedException e) {
				// Weirdness - SiteService pulled this id and now it
				// can't find the site it pulled it from
				LOG.error("IdUnusedException trying to get site to remove non-read access messasges.");
				continue;
			}

			int pos = indexOf(site.getId(), getSiteIds(removeMessageCounts));

			// permissions based on roles, so need to check if user's role has messages
			// that need to be removed from totals (either total or unread)
			if (pos != -1) {
				resultSet = (Object []) removeMessageCounts.get(pos);
				
				while (site.getId().equals((String) resultSet[0])) {
					final String curRole = AuthzGroupService.getUserRole(
												SessionManager.getCurrentSessionUserId(),
														("/site/" + site.getId()) );
				
					if (curRole.equals((String) resultSet[1])) {
						resultList.add(resultSet);
					
						// remove all rows of removeMessageCounts for this site
						// since I've found the one I was looking for
						while (site.getId().equals((String) resultSet[0])) {
							removeMessageCounts.remove(pos++);
						
							if (pos < removeMessageCounts.size()) {
								resultSet = (Object []) removeMessageCounts.get(pos);
							}
							else {
								resultSet[0] = "";
							}
						}
					}
					else {
						// this row is not it but may have others so remove it
						// and set up for next iteration of the loop
						removeMessageCounts.remove(pos++);

						if (pos < removeMessageCounts.size()) {
							resultSet = (Object []) removeMessageCounts.get(pos);
						}
						else {
							resultSet[0] = "";
						}
					} 
				}
			}
		}
		
		return resultList;
	}

	/**
	 * Determines the number of unread messages for each site.
	 * Filters out messages user does not have read permission for.
	 * 
	 * @param siteList
	 * 			List of sites user is a member of/has access to
	 * 
	 * @return
	 * 		List of unread message counts grouped by site
	 */
	private List compileDFMessageCount(List siteList) {
		// retrieve what possible roles user could be in sites
		final List roleList = getUserRoles(siteList);
		
		// ******* Pulls discussion forum message counts from DB *******
		List unreadDFMessageCounts = new ArrayList();
		List discussionForumMessageCounts = messageManager
						.findDiscussionForumMessageCountsForAllSites(siteList);

		// if no messages at all, no need for further processing
		if (! discussionForumMessageCounts.isEmpty()) {
			List discussionForumRemoveMessageCounts = messageManager
						.findDiscussionForumMessageRemoveCountsForAllSites(siteList, roleList);

			discussionForumRemoveMessageCounts = selectCorrectRemoveMessageCount(
													discussionForumRemoveMessageCounts, siteList);

			// if no read messages to remove, don't bother
			if (! discussionForumRemoveMessageCounts.isEmpty()) {
				discussionForumMessageCounts = filterNoAccessMessages(
													discussionForumMessageCounts,
													discussionForumRemoveMessageCounts);
			}

			// if no messages left, no read messages to get
			if (! discussionForumMessageCounts.isEmpty()) {
				// Pulls read discussion forum message counts from DB
				List discussionForumReadMessageCounts = messageManager
											.findDiscussionForumReadMessageCountsForAllSites();

				// if no read messages, don't bother
				if (! discussionForumReadMessageCounts.isEmpty()) {
					List discussionForumRemoveReadMessageCounts = messageManager
										.findDiscussionForumReadMessageRemoveCountsForAllSites(getUserRoles(siteList));

					discussionForumRemoveReadMessageCounts = 
								selectCorrectRemoveMessageCount(discussionForumRemoveReadMessageCounts, siteList);

					// if no messages to remove, don't bother
					if (! discussionForumRemoveReadMessageCounts.isEmpty()) {
						discussionForumReadMessageCounts = filterNoAccessMessages(
																discussionForumReadMessageCounts,
																discussionForumRemoveReadMessageCounts);
					}

					if (! discussionForumReadMessageCounts.isEmpty()) {
						unreadDFMessageCounts = computeUnreadDFMessages(
													discussionForumMessageCounts,
													discussionForumReadMessageCounts);
					} 
					else {
						// after filtering, no unread message counts
						unreadDFMessageCounts = discussionForumMessageCounts;
					}
				}
				else {
					// no read message counts
					unreadDFMessageCounts = discussionForumMessageCounts;
				}
			}
		}
		
		return unreadDFMessageCounts;
	}

	/**
	 * Removes all sites user does not want message info about and
	 * returns all sites left
	 * 
	 * @param allSites
	 * 				List of all sites user is a member of
	 * 
	 * @return
	 * 		List of sites user wants notification about
	 */
	private List filterOutExcludedSites(List allSites) {
		final List excludedSites = getExcludedSiteList();
		
		if (excludedSites != null) {
			for (Iterator excludeIter = excludedSites.iterator(); excludeIter.hasNext(); ) {
				final String siteId = (String) excludeIter.next();
				final int pos = indexOf(siteId, allSites);
			
				if (pos != -1) {
					allSites.remove(pos);
				}
			}
		}
		
		return allSites;
	}
	
	/**
	 * Return List to populate page if in MyWorkspace
	 * 
	 * @return
	 * 		List of DecoratedCompiledMessageStats to populate MyWorkspace page
	 */
	private List getMyWorkspaceContents() {
		final boolean firstIteration = true;
		final List contents = new ArrayList();
		Object[] unreadDFCount;
		Object[] pmCounts;
		
		// Used to determine if there are any sites to view on UI
		sitesToView = false;

		// retrieve what sites is this user a member of
		final List siteList = filterOutExcludedSites(getSiteList());

		// no sites to work with, set boolean variable and return
		if (siteList.isEmpty()) { 
			sitesToView = false;
			return contents; 
		}

		// ******* Pulls unread private message counts from DB ******* 
		final List privateMessageCounts = pvtMessageManager
					.getPrivateMessageCountsForAllSites();

		// ******* Pulls unread discussion forum message counts from DB *******
		List unreadDFMessageCounts = compileDFMessageCount(siteList);
		
		// If both are empty, no unread messages so
		// create 0 count beans for both types for all sites not filtered so
		// displays proper messages
		if (privateMessageCounts.isEmpty() && unreadDFMessageCounts.isEmpty()) {
			
			for (Iterator siteIter = siteList.iterator(); siteIter.hasNext(); ) {
				String siteId = "";
				Site site= null;
			
				// ************ Get next site from List ************ 
				try {
					siteId = (String) siteIter.next();
					site = getSite(siteId);
				}
				catch (IdUnusedException e) {
					// Wierdness has happened - pulled from SiteService but now can't
					// find it. Log and skip
					LOG.error("IdUnusedException attempting to access site " + siteId);
					continue;
				}
				
				// ************ Each row on page gets info stored in DecoratedCompiledMessageStats bean ************ 
				final DecoratedCompiledMessageStats dcms = new DecoratedCompiledMessageStats();

				// fill site title
				dcms.setSiteName(site.getTitle());
				dcms.setSiteId(site.getId());
				
				dcms.setUnreadForumsAmt(0);
				dcms.setUnreadPrivateAmt(0);
				
				dcms.setMcPageURL(getMCPageURL(site.getId()));

				contents.add(dcms);
				
				sitesToView = true;
			}
			
			return contents;
		}

		//============= At least some unread messages so process =============

		// ************ loop through info to fill decorated bean ************ 
		for (Iterator si = siteList.iterator(); si.hasNext();) {
			boolean hasPrivate = false;
			boolean hasDF = false;
			String siteId = "";
			Site site= null;
		
			// ************ Get next site from List ************ 
			try {
				siteId = (String) si.next();
				site = getSite(siteId);
			}
			catch (IdUnusedException e) {
				// Wierdness has happened - pulled from SiteService but now can't
				// find it. Log and skip
				LOG.error("IdUnusedException attempting to access site " + siteId);
				continue;
			}

			// Determine if current site has unread private messages
			final int PMpos = indexOf(siteId, getSiteIds(privateMessageCounts));
			
			if (PMpos != -1) {
				pmCounts = (Object []) privateMessageCounts.get(PMpos);
				
				// to make searching for remaining counts more efficient
				privateMessageCounts.remove(pmCounts);
			}
			else {
				pmCounts = new Object[1];
				pmCounts[0] = "";
			}

			// Determine if current site has unread discussion forum messages
			final int DFpos = indexOf(siteId, getSiteIds(unreadDFMessageCounts));
			
			if (DFpos != -1) {
				unreadDFCount = (Object []) unreadDFMessageCounts.get(DFpos);
				
				// to make searching for remaining counts more efficient
				unreadDFMessageCounts.remove(DFpos);
			}
			else {
				unreadDFCount = new Object[1];
				unreadDFCount[0] = "";
			}

			// ************ Each row on page gets info stored in DecoratedCompiledMessageStats bean ************ 
			final DecoratedCompiledMessageStats dcms = new DecoratedCompiledMessageStats();

			// fill site title
			dcms.setSiteName(site.getTitle());
			dcms.setSiteId(site.getId());

			// Put check here because if not in site, skip
			if (isMessageForumsPageInSite(site)) {

				// ************ checking for unread private messages for this site ************  
				if (siteId.equals(pmCounts[0])) {
					// check if not enabled
					final Area area = areaManager.getAreaByContextIdAndTypeId(site.getId(), 
													typeManager.getPrivateMessageAreaType());
					
					if (area != null) {
						if (area.getEnabled().booleanValue()) {
							dcms.setUnreadPrivateAmt(((Integer) pmCounts[1]).intValue());
							hasPrivate = true;
						}
						else {
							dcms.setUnreadPrivateAmt(0);
							hasPrivate = true;
						}
					}
					else {
						dcms.setUnreadPrivateAmt(0);
						hasPrivate = true;
					}
				}
				

				// ************ check for unread discussion forum messages on this site ************
				if (siteId.equals(unreadDFCount[0])) {
					dcms.setUnreadForumsAmt(((Integer) unreadDFCount[1]).intValue());

					hasDF = true;
				} 
				else {
					if (areaManager.getDiscusionArea().getEnabled().booleanValue()) {
						dcms.setUnreadForumsAmt(0);
						hasDF = true;
					}
				}

				// ************ get the page URL for Message Center************
				// only if unread messages, ie, only if row will appear on page 
				if (hasPrivate || hasDF) {
					dcms.setMcPageURL(getMCPageURL(site.getId()));

					contents.add(dcms);
					
					sitesToView = true;
				}
			}
		}
		
		return contents;
	}

	/**
	 * Returns a list of Strings stored in index 0 of Object [] members of
	 * list passed in.
	 * 
	 * @param counts
	 * 			List of Object [] members whose element at index 0 is a String
	 * 
	 * @return
	 * 			List of Strings extracted from list passed in
	 */
	private List getSiteIds(List counts) {
		List results = new ArrayList();
		
		if (! counts.isEmpty()) {
			for (Iterator iter = counts.iterator(); iter.hasNext(); ) {
				Object [] pmCount = (Object []) iter.next();
				
				results.add(pmCount[0]);
			}
		}
		
		return results;
	}
	
	/**
	 * Returns List to populate page if on Home page of a site
	 * 
	 * @return
	 * 		List of DecoratedCompiledMessageStats for a particular site
	 */
	private List getSiteContents() {
		List contents = new ArrayList();
		
		// Check if tool within site
		// if so, get stats for just this site
		if (isMessageForumsPageInSite()) {
			int unreadPrivate = 0;

			DecoratedCompiledMessageStats dcms = new DecoratedCompiledMessageStats();

			dcms.setSiteName(getSiteName());

			// Get private message area so we can get the private messasge forum so we can get the
			// List of topics so we can get the Received topic to finally determine number of unread messages
			final Area area = pvtMessageManager.getPrivateMessageArea();
			
			if (pvtMessageManager.getPrivateMessageArea().getEnabled().booleanValue()) {
				PrivateForum pf = pvtMessageManager.initializePrivateMessageArea(area);
				
				unreadPrivate = pvtMessageManager.findUnreadMessageCount(
									typeManager.getReceivedPrivateMessageType());

				dcms.setUnreadMessages(unreadPrivate);
				dcms.setUnreadPrivateAmt(unreadPrivate);
			}
			else {
				dcms.setUnreadMessages(0);
			}

			dcms.setHeading(rb.getString(PRIVATE_HEADING));
			dcms.setMcPageURL(getMCPageURL());

			contents.add(dcms);
			
			dcms = new DecoratedCompiledMessageStats();

			// Number of unread forum messages is a little harder
			// need to loop through all topics and add them up
			final List topicsList = forumManager.getDiscussionForums();
			int unreadForum = 0;

			final Iterator forumIter = topicsList.iterator();

			while (forumIter.hasNext()) {
				final DiscussionForum df = (DiscussionForum) forumIter.next();

				final List topics = df.getTopics();
				final Iterator topicIter = topics.iterator();

				while (topicIter.hasNext()) {
					final Topic topic = (Topic) topicIter.next();
					
					if (uiPermissionsManager.isRead((DiscussionTopic) topic, df)) {
						unreadForum += messageManager.findUnreadMessageCountByTopicId(topic.getId());
					}
				}
			}
			dcms.setUnreadMessages(unreadForum);
			dcms.setHeading(rb.getString(DISCUSSION_HEADING));
			dcms.setMcPageURL(getMCPageURL());

			contents.add(dcms);
		}
		else {
			// TODO: what to put on page? Alert? Leave Blank?
		}
		
		return contents;
	}

	/**
	 * Returns List of decoratedCompiledMessageStats. Called by
	 * jsp page and main processing of list to be displayed.
	 * <p>
	 * Used by both MyWorkspace and site Home page.
	 * 
	 * @return 
	 * 		List of decoratedCompiledMessageStats
	 */
	public List getContents() {
		if (isMyWorkspace()) {
			// Get stats for "all" sites this user is a member of
			// and has not turned displaying info off
			return getMyWorkspaceContents();
		}
		else {
			return getSiteContents();
		}
	}

	/**
	 * Retrieve the site display title
	 * 
	 * @return
	 * 		String of the title of the site
	 */
	private String getSiteName() {
		try {
			return getSite(getContext()).getTitle();

		} 
		catch (IdUnusedException e) {
			LOG.error("IdUnusedException when trying to access site "
					+ e.getMessage());
		}

		return null;
	}

	/**
	 * Returns the site id String
	 * 
	 * @return 
	 * 		The id for current site
	 */
	private String getSiteId() {
		try {
			return getSite(getContext()).getId();

		} 
		catch (IdUnusedException e) {
			LOG.error("IdUnusedException when trying to access site "
					+ e.getMessage());
		}

		return null;
	}

	/**
	 * Returns List of unread messages organized by site
	 * 
	 * @param totalMessages
	 * 			List of all messages by site
	 * 
	 * @param readMessages
	 * 			List of all read messages by site
	 * 
	 * @param totalNoAccessMessages
	 * 			List of all messages user does not have access to by site
	 * 
	 * @pararm totalNoAccessReadMessages
	 * 			List of all read messages user does not have access to by site
	 * 			(ie, no read permission for that topic)
	 * 
	 * @return
	 * 			List of unread messages by site
	 */
	private List computeUnreadDFMessages(List totalMessages, List readMessages) {
		List unreadDFMessageCounts = new ArrayList();

		final List readSiteIds = getSiteIds(readMessages);
		
		// Constructs the unread message counts from above 4 lists
		final Iterator dfMessagesIter = totalMessages.iterator();
		
		// NOTE: dfMessagesIter.count >= dfReadMessagesIter, so use
		// dfMessagesIter for compilation loop
		while (dfMessagesIter.hasNext()) {
			final Object [] dfMessageCountForASite = (Object[]) dfMessagesIter.next();
			
			final Object[] siteDFInfo = new Object[2];

			siteDFInfo[0] = (String) dfMessageCountForASite[0];

			int pos = indexOf((String) siteDFInfo[0], readSiteIds);
			
			if (pos != -1) {
				final Object [] dfReadMessageCountForASite = (Object []) readMessages.get(pos);
				
				siteDFInfo[1] = new Integer(((Integer) dfMessageCountForASite[1]).intValue()
												- ((Integer) dfReadMessageCountForASite[1]).intValue());
			} 
			else {
				// No messages read for this site so message count = unread message count
				siteDFInfo[1] = (Integer) dfMessageCountForASite[1];
			}

			unreadDFMessageCounts.add(siteDFInfo);
		}

		return unreadDFMessageCounts;
	}

	/**
	 * Returns TRUE if Message Forums (Message Center) exists in this site,
	 * FALSE otherwise Called if tool placed on home page of a site
	 * 
	 * @return TRUE if Message Forums (Message Center) exists in this site,
	 *         FALSE otherwise
	 */
	public boolean isMessageForumsPageInSite() {
		boolean mfToolExists = false;

		try {
			final Site thisSite = getSite(getContext());

			mfToolExists = isMessageForumsPageInSite(thisSite);

		} catch (IdUnusedException e) {
			LOG.error("IdUnusedException while trying to check if site has MF tool.");
		}

		return mfToolExists;
	}

	/**
	 * Returns TRUE if Message Forums (Message Center) exists in this site,
	 * FALSE otherwise Called if tool placed on My Workspace
	 * 
	 * @return TRUE if Message Forums (Message Center) exists in this site,
	 *         FALSE otherwise
	 */
	private boolean isMessageForumsPageInSite(Site thisSite) {
		Collection toolsInSite = thisSite.getTools(MESSAGE_CENTER_ID);

		return ! toolsInSite.isEmpty();
	}

	/**
	 * Returns the URL for the page the Message Center tool is on. Called if
	 * tool on home page of a site.
	 * 
	 * @return String A URL so the user can click to go to Message Center.
	 *         Needed since tool could possibly by in MyWorkspace
	 */
	private String getMCPageURL() {
		return getMCPageURL(getContext());
	}

	/**
	 * Returns the URL for the page the Message Center tool is on.
	 * 
	 * @return String A URL so the user can click to go to Message Center.
	 *         Needed since tool could possibly by in MyWorkspace
	 */
	private String getMCPageURL(String siteId) {
		try {
			Collection toolsInSite = getSite(siteId).getTools(MESSAGE_CENTER_ID);
			ToolConfiguration mcTool;

			if (!toolsInSite.isEmpty()) {
				Iterator iter = toolsInSite.iterator();
				mcTool = (ToolConfiguration) iter.next();

				SitePage pgelement = mcTool.getContainingPage();

				return pgelement.getUrl();
			}

		}
		catch (IdUnusedException e) {
			// Weirdness since site ids used gotten from SiteService
			LOG.error("IdUnusedException while trying to check if site has MF tool.");

			// TODO: What do we do?
		}

		return "";

	}

	/**
	 * This marks all Private messages as read for a particular site
	 * 
	 * @param ActionEvent e
	 */
	public void processReadAll(ActionEvent e) {
		final String typeUuid = typeManager.getReceivedPrivateMessageType();

		if (isMyWorkspace()) {
			// if within MyWorkspace, need to find the siteId
			final FacesContext context = FacesContext.getCurrentInstance();
			final Map requestParams = context.getExternalContext()
												.getRequestParameterMap();

			final String contextId = (String) requestParams.get(CONTEXTID);

			final List privateMessages = pvtMessageManager
											.getMessagesByTypeByContext(typeUuid, contextId);

			if (privateMessages == null) {
				LOG.error("No messages found while attempting to mark all as read "
								+ "from synoptic Message Center tool.");
			} 
			else {
				for (Iterator iter = privateMessages.iterator(); iter.hasNext();) {
					pvtMessageManager.markMessageAsReadForUser(
											(PrivateMessage) iter.next(), contextId);
				}
			}
		} 
		else {
			// Get the site id and user id and call query to
			// mark them all as read
			List privateMessages = pvtMessageManager.getMessagesByType(
										typeUuid, PrivateMessageManager.SORT_COLUMN_DATE,
											PrivateMessageManager.SORT_DESC);

			if (privateMessages == null) {
				LOG.error("No messages found while attempting to mark all as read "
								+ "from synoptic Message Center tool.");
			} 
			else {
				for (Iterator iter = privateMessages.iterator(); iter.hasNext();) {
					pvtMessageManager.markMessageAsReadForUser((PrivateMessage) iter.next());
				}
			}
		}
	}

	/**
	 * Returns the Site object for this id, if it exists.
	 * If not, returns IdUnusedException
	 * 
	 * @param siteId
	 * 			The site id to check
	 * 
	 * @return
	 * 			Site object for this id
	 */
	private Site getSite(String siteId) 
		throws IdUnusedException {
		return SiteService.getSite(siteId);
	}
	
	/**
	 * Returns current context
	 * 
	 * @return
	 * 		String The site id (context) where tool currently located
	 */
	private String getContext() {
		return ToolManager.getCurrentPlacement().getContext();
	}
	
	/**
	 * 
	 * @return
	 * 		List A List of site ids that is published and the user is a member of
	 */
	public List getSiteList() {
		List mySites = SiteService.getSites(org.sakaiproject.site.api.SiteService.SelectionType.ACCESS,
				null,null,null,org.sakaiproject.site.api.SiteService.SortType.ID_ASC,
				null);

		Iterator lsi = mySites.iterator();

		if (!lsi.hasNext()) {
			LOG.warn("User " + SessionManager.getCurrentSessionUserId() + " does not belong to any sites.");

			return mySites;
		}

		final List siteList = new ArrayList();

		// only display sites that are published and have Message Center in them
		while (lsi.hasNext()) {
			Site site = (Site) lsi.next();

			// filter out unpublished or no messsage center
			if (site.isPublished() && isMessageForumsPageInSite(site)) {
				siteList.add(site.getId());
			}
		}

		return siteList;
	}

	// ======================== Options Page Methods ======================== 

	public List getNonNotificationSitesItems() {
		return nonNotificationSitesItems;
	}

	public void setNonNotificationSitesItems(List nonNotificationSites) {
		this.nonNotificationSitesItems = nonNotificationSites;
	}

	public String[] getNonNotificationSites() {
		return nonNotificationSites;
	}

	public void setNonNotificationSites(String[] nonNotificationSites) {
		this.nonNotificationSites = nonNotificationSites;
	}

	public List getNotificationSitesItems() {
		return notificationSitesItems;
	}

	public void setNotificationSitesItems(List notificationSitesItems) {
		this.notificationSitesItems = notificationSitesItems;
	}

	public String[] getNotificationSites() {
		return notificationSites;
	}

	public void setNotificationSites(String[] notificationSites) {
		this.notificationSites = notificationSites;
	}

	/**
	 * Returns list of sites user does NOT want unread message notifications
	 * about
	 * 
	 * @return
	 * 		List of sites user does NOT want unread message notifications about
	 */
	private List getExcludedSiteList() {
		Preferences prefs = preferencesService.getPreferences(
									SessionManager.getCurrentSessionUserId());
		
		ResourceProperties props = prefs.getProperties(SYNMC_OPTIONS_PREFS);
		final List l = props.getPropertyList(EXCLUDE_STRING);

		return l;
	}

	/**
	 * Loads the List of sites to display info and those to not
	 * before moving to Options page.
	 * 
	 * @return
	 * 		String to move to Option page
	 */
	public String processGotoOptions() {
		final List sites = getSiteList();
		
		notificationSitesItems.clear();
		nonNotificationSitesItems.clear();
		List prefExclude = new Vector();

		// Get excluded sites, if any. If returns null,
		// use empty list prefExclude, otherwise results returned.
		final List l = getExcludedSiteList();
		if (l != null) {
			prefExclude = l;
		}

		// create excluded and order list of Sites and add balance mySites to excluded Site list for display in Form
		List excluded = new Vector();

		for (Iterator iter = prefExclude.iterator(); iter.hasNext();) {
			final String element = (String) iter.next();
			final int pos = indexOf(element, sites);

			if (pos != -1) {
				try {
					Site s = getSite((String) sites.get(pos));

					excluded.add(s);
					sites.remove(pos);
				} 
				catch (IdUnusedException e) {
					// Weirdness since site ids pulled from SiteService
					LOG.error("IdUnusedException while getting site when creating actual excluded site list");
				}
			}
		}

		// Now convert to SelectItem for display in JSF
		for (Iterator iter = excluded.iterator(); iter.hasNext();) {
			Site element = (Site) iter.next();
			
			SelectItem excludeItem = new SelectItem(
											element.getId(), element.getTitle());

			nonNotificationSitesItems.add(excludeItem);
		}

		for (Iterator iter = sites.iterator(); iter.hasNext();) {
			Site element = null;

			try {
				element = getSite((String) iter.next());
			} 
			catch (IdUnusedException e) {
				// Weirdness, pulled by SiteService as a valid site but when try
				// to retrieve throws exception
				LOG.error("IdUnusedException while trying to load site for "
								+ "Message Center Notifications Options page.");

				// skip this invalid site
				continue;
			}

			SelectItem excludeItem = new SelectItem(
										element.getId(), element.getTitle());

			notificationSitesItems.add(excludeItem);
		}

		return "synOptions";
	}

	/**
	 * Move site(s) from not displaying unread info to displaying
	 * 
	 * @return
	 * 		String to stay on Options page
	 */
	public String processActionAdd() {
		String[] values = getNonNotificationSites();

		if (values.length < 1) {
			setErrorMessage(NO_SITE_SELECTED_MSG);
		}

		for (int i = 0; i < values.length; i++) {
			String value = values[i];
			getNotificationSitesItems().add(
					removeItems(value, getNonNotificationSitesItems()));
		}

		return "synOption";
	}

	/**
	 * Move site(s) from getting displaying unread info to not
	 * 
	 * @return
	 * 		String to stay on Options page
	 */
	public String processActionRemove() {
		String[] values = getNotificationSites();

		if (values.length < 1) {
			setErrorMessage(NO_SITE_SELECTED_MSG);
		}

		for (int i = 0; i < values.length; i++) {
			String value = values[i];
			getNonNotificationSitesItems().add(
					removeItems(value, getNotificationSitesItems()));
		}

		return "synOption";
	}

	/**
	 * Process Add All action
	 * 
	 * @return navigation output to tab customization page (edit)
	 */
	public String processActionAddAll() {
		LOG.debug("processActionAddAll()");

		getNotificationSitesItems().addAll(getNonNotificationSitesItems());
		getNonNotificationSitesItems().clear();

		return "synOptions";
	}

	/**
	 * Process Add All action
	 * 
	 * @return navigation output to tab customization page (edit)
	 */
	public String processActionRemoveAll() {
		LOG.debug("processActionAddAll()");

		getNonNotificationSitesItems().addAll(getNotificationSitesItems());
		getNotificationSitesItems().clear();

		return "synOptions";
	}

	/**
	 * Change display options for synoptic Message Center screen
	 * 
	 * @return String to handle navigation
	 */
	public String processOptionsChange() {
		LOG.debug("processOptionsSave()");

		setUserEditingOn();

		// Remove existing property
		ResourcePropertiesEdit props = prefsEdit.getPropertiesEdit(SYNMC_OPTIONS_PREFS);
		props.removeProperty(EXCLUDE_STRING);

		// Commit to remove from database, for next set of value storing
		preferencesService.commit(prefsEdit);

		prefsEditKNVCollection = new Vector();
		String eparts = "";

		for (int i = 0; i < nonNotificationSitesItems.size(); i++) {
			SelectItem item = (SelectItem) nonNotificationSitesItems.get(i);
			
			String evalue = (String) item.getValue();
			eparts += evalue + ", ";
		}

		// add property name and value for saving
		prefsEditKNVCollection.add(new KeyNameValue(
							SYNMC_OPTIONS_PREFS, EXCLUDE_STRING, eparts, true));

		// save
		saveEdit();

		// release lock and clear session variables
		cancelEdit();

		return "synMain";
	}

	/**
	 * Cancel changes to display settings for synoptic Message Center screen
	 * 
	 * @return String to return to main page
	 */
	public String processOptionsCancel() {
		LOG.debug("processOptionsCancel()");

		// remove session variables
		cancelEdit();

		return "synMain";
	}

	/**
	 * Clean up information when user canels changes
	 */
	protected void cancelEdit() {
		LOG.debug("cancelEdit()");

		// cleanup
		prefsEditKNVCollection = null;
		prefsEdit = null;
		nonNotificationSitesItems = new ArrayList();

	}

	/**
	 * Used by processActionAdd() and processActionRemove() to move site
	 * from one list to the other
	 * 
	 * @param value
	 * 			The site to be moved
	 * 
	 * @param items
	 * 			Where the site currently lives
	 * 
	 * @return
	 * 		The SelectItem removed from items
	 */
	private SelectItem removeItems(String value, List items) {
		if (LOG.isDebugEnabled()) {
			LOG.debug("removeItems(String " + value + ", List " + items + ")");
		}

		SelectItem result = null;
		for (int i = 0; i < items.size(); i++) {
			SelectItem item = (SelectItem) items.get(i);
			
			if (value.equals(item.getValue())) {
				result = (SelectItem) items.remove(i);
				break;
			}
		}
		return result;
	}

	/**
	 * Passes an error message to the Spring framework to display on page.
	 * 
	 * @param alertMsg The key to get the message from the message bundle 
	 */
	private void setErrorMessage(String alertMsg) {
		FacesContext.getCurrentInstance().addMessage(null,
									new FacesMessage(rb.getString(alertMsg)));
	}

	/**
	 * Set editing mode on for user and add user if not existing
	 */
	protected void setUserEditingOn() {
		LOG.debug("setUserEditingOn()");

		try {
			prefsEdit = preferencesService.edit(SessionManager
													.getCurrentSessionUserId());
		} 
		catch (IdUnusedException e) {
			try {
				prefsEdit = preferencesService.add(SessionManager
													.getCurrentSessionUserId());
			} 
			catch (Exception ee) {
				// PermissionException or IdUnusedException
				LOG.error(ee.getMessage() + " occured while attempting to add new user preferences.");
			}
		} 
		catch (Exception e) {
			// Any other error besides IdUnusedException should be an error
			LOG.error(e.getMessage() + " occured while attempting to edit user preferences.");
		}
	}

	/**
	 * Save any changed values from the edit and cleanup.
	 */
	protected void saveEdit() {
		LOG.debug("saveEdit()");

		// user editing is required as commit() disable isActive() flag
		setUserEditingOn();

		// move the stuff from prefsEditKNVCollection into the edit
		for (Iterator i = prefsEditKNVCollection.iterator(); i.hasNext();) {
			KeyNameValue knv = (KeyNameValue) i.next();

			// find the original to remove (unless this one was new)
			if (!knv.getOrigKey().equals("")) {
				ResourcePropertiesEdit props = prefsEdit.getPropertiesEdit(knv
																	.getOrigKey());
				props.removeProperty(knv.getOrigName());
			}
			// add the new if we have a key and name and value
			if ((!knv.getKey().equals("")) && (!knv.getName().equals(""))
					&& (!knv.getValue().equals(""))) {
				ResourcePropertiesEdit props = prefsEdit.getPropertiesEdit(knv.getKey());
				
				if (knv.isList()) {
					// split by ", "
					String[] parts = knv.getValue().split(", ");
					for (int p = 0; p < parts.length; p++) {
						props.addPropertyToList(knv.getName(), parts[p]);
					}
				} 
				else {
					props.addProperty(knv.getName(), knv.getValue());
				}
			}
		}
		// save the preferences, release the edit
		preferencesService.commit(prefsEdit);
	}

	/**
	 * Find the site in the list that has this id - return the position. *
	 * 
	 * @param value
	 *        The site id to find.
	 * @param siteList
	 *        The list of Site objects.
	 * @return The index position in siteList of the site with site id = value, or -1 if not found.
	 */
	protected int indexOf(String value, List siteList) {
		if (LOG.isDebugEnabled()) {
			LOG.debug("indexOf(String " + value + ", List " + siteList + ")");
		}

		for (int i = 0; i < siteList.size(); i++) {
			String siteId = (String) siteList.get(i);

			if (siteId.equals(value)) {
				return i;
			}
		}
		return -1;
	}

}
