import React, { createContext, useContext, useState, useEffect } from 'react';
import { getFeatures } from '../api/configApi';

const FeatureContext = createContext(null);

export const FeatureProvider = ({ children }) => {
    const [features, setFeatures] = useState({
        voiceEnabled: false,
        gradioConfigured: false,
    });
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        const fetchFeatures = async () => {
            try {
                const response = await getFeatures();
                if (response.success) {
                    setFeatures(response.data);
                }
            } catch (err) {
                console.error('Failed to fetch features:', err);
                // 실패 시 기본값 유지 (음성 기능 비활성화)
            } finally {
                setLoading(false);
            }
        };

        fetchFeatures();
    }, []);

    return (
        <FeatureContext.Provider value={{ ...features, loading }}>
            {children}
        </FeatureContext.Provider>
    );
};

export const useFeatures = () => {
    const context = useContext(FeatureContext);
    if (!context) {
        throw new Error('useFeatures must be used within a FeatureProvider');
    }
    return context;
};
