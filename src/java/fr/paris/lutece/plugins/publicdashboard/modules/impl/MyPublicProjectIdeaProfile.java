/*
 * Copyright (c) 2002-2022, City of Paris
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  1. Redistributions of source code must retain the above copyright notice
 *     and the following disclaimer.
 *
 *  2. Redistributions in binary form must reproduce the above copyright notice
 *     and the following disclaimer in the documentation and/or other materials
 *     provided with the distribution.
 *
 *  3. Neither the name of 'Mairie de Paris' nor 'Lutece' nor the names of its
 *     contributors may be used to endorse or promote products derived from
 *     this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * License 1.0
 */
package fr.paris.lutece.plugins.publicdashboard.modules.impl;

import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import fr.paris.lutece.plugins.dashboard.service.Action;
import fr.paris.lutece.plugins.dashboard.service.PublicDashboardService;
import fr.paris.lutece.plugins.dashboard.service.PublicUserProfile;
import fr.paris.lutece.plugins.forms.business.Form;
import fr.paris.lutece.plugins.forms.business.FormHome;
import fr.paris.lutece.plugins.forms.business.FormQuestionResponse;
import fr.paris.lutece.plugins.forms.business.FormQuestionResponseHome;
import fr.paris.lutece.plugins.forms.business.FormResponse;
import fr.paris.lutece.plugins.forms.business.FormResponseHome;
import fr.paris.lutece.plugins.forms.business.FormResponseStep;
import fr.paris.lutece.plugins.forms.service.FormResponseService;
import fr.paris.lutece.plugins.forms.web.FormResponseData;
import fr.paris.lutece.plugins.genericattributes.business.Response;
import fr.paris.lutece.portal.service.plugin.Plugin;
import fr.paris.lutece.portal.service.security.LuteceUser;
import fr.paris.lutece.portal.service.security.RsaService;
import fr.paris.lutece.portal.service.security.SecurityService;
import fr.paris.lutece.portal.service.spring.SpringContextService;
import fr.paris.lutece.portal.service.util.AppLogService;
import fr.paris.lutece.portal.service.util.AppPropertiesService;
import fr.paris.lutece.util.password.IPasswordFactory;

public class MyPublicProjectIdeaProfile extends PublicUserProfile
{

    private static final String CONST_KEY_PROPERTIES = "myformreponse.code";

    private MyPublicProjectIdeaProfile( UserProfileBuilder builder )
    {
        super( builder );
    }

    // builder
    public static class UserProfileBuilder extends PublicUserProfile.UserProfileBuilder
    {
        public UserProfileBuilder( String userid )
        {
            super( userid );
        }

        @Override
        public UserProfileBuilder withActions( String guid )
        {
            _listActions = searchParticipationIdea( guid );
            return this;
        }

    }

    private static List<Action> searchParticipationIdea( String guid )
    {

        if ( AppPropertiesService.getPropertyBoolean( PublicDashboardService.PROPERTY_ENCRYPT, false ) )
        {
            try
            {
                guid = RsaService.decryptRsa( guid );
            }
            catch( GeneralSecurityException e )
            {
                AppLogService.error( "Cannot decrypt " + guid, e );
            }
        }

        List<Action> listActions = new ArrayList<>( );

        Map<String, Map<String, String>> mapResponsesValuesByFormResponse = new HashMap<String, Map<String, String>>( );

        LuteceUser user = new LuteceUser( guid, SecurityService.getInstance( ).getAuthenticationService( ) )
        {
        };
        FormResponseService formResponseService = FormResponseService.getInstance( );

        List<FormResponseData> lstFormData = formResponseService.getFormResponseListForUser( user );
        for ( FormResponseData responseData : lstFormData )
        {
            FormResponse formRep = FormResponseHome.findByPrimaryKey( responseData.getIdFormResponse( ) );
            List<FormResponseStep> lstStep = formRep.getSteps( );
            Map<String, String> mapResponsesValues = new HashMap<String, String>( );
            for ( FormResponseStep step : lstStep )
            {
                List<FormQuestionResponse> lstQr = step.getQuestions( );
                for ( FormQuestionResponse fqr : lstQr )
                {
                    if ( fqr.getQuestion( ).isPublished( ) )
                    {
                        List<Response> lstresp = fqr.getEntryResponse( );
                        for ( Response resp : lstresp )
                        {
                            mapResponsesValues.put( fqr.getQuestion( ).getTitle( ), resp.getResponseValue( ) );
                        }
                    }
                }
            }
            if ( !mapResponsesValues.isEmpty( ) )
            {
                mapResponsesValuesByFormResponse.put( String.valueOf( responseData.getIdFormResponse( ) ), mapResponsesValues );
            }
        }

        for ( Map.Entry<String, Map<String, String>> entry : mapResponsesValuesByFormResponse.entrySet( ) )
        {
            for ( Map.Entry<String, String> entryMap : entry.getValue( ).entrySet( ) )
            {
                listActions.add( new Action( entryMap.getKey( ), entryMap.getValue( ) ) );
            }
        }

        return listActions;

    }

}
