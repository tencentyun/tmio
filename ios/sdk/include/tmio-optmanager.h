#pragma once

#include <stdint.h>

#include <map>
#include <set>
#include <sstream>
#include <string>
#include <utility>

namespace tmio {

template <typename T>
class OptionManager {
public:
    bool setOptionValue(const std::string &optname, T value) {
        auto item = option_map_.find(optname);
        // check optname is valid
        if (item == option_map_.end()) {
            return false;
        }

        // check in [min, max] range
        if (option_min_max_range_.count(optname) != 0) {
            auto range = option_min_max_range_[optname];
            if (value < range.first || value > range.second) {
                return false;
            }
        }

        // check in [v1, v2, v3, ...] range
        if (option_enum_range_.count(optname) != 0) {
            if (option_enum_range_[optname].count(value) == 0) {
                return false;
            }
        }

        item->second(&value);
        return true;
    }

    bool getOptionValue(const std::string &optname, T *value) {
        auto item = option_map_.find(optname);
        // check optname is valid
        if (item == option_map_.end()) {
            return false;
        }

        *value = item->second(nullptr);
        return true;
    }

    using SetGetCallback = std::function<T(T *)>;

    void registerOption(std::string optname, SetGetCallback cb) {
        option_map_.emplace(optname, cb);
    }

    template <typename N>
    void registerOption(std::string optname, N *ptr) {
        auto cb = [ptr](T *new_value) {
            if (new_value != nullptr) {
                *ptr = static_cast<N>(*new_value);
            }
            return static_cast<T>(*ptr);
        };
        option_map_.emplace(optname, cb);
    }

    template <typename N>
    void registerOption(std::string optname, N *ptr, T min, T max) {
        registerOption(optname, ptr);
        option_enum_range_.erase(optname);
        option_min_max_range_.emplace(std::move(optname),
                                      std::make_pair(min, max));
    }

    template <typename N>
    void registerOption(std::string optname, N *ptr, std::set<T> range) {
        registerOption(optname, ptr);
        option_min_max_range_.erase(optname);
        option_enum_range_.emplace(std::move(optname), std::move(range));
    }

    std::string dumpOption() {
        std::ostringstream ss;
        for (const auto &item : option_map_) {
            ss << item.first;
            if (option_min_max_range_.count(item.first)) {
                auto range = option_min_max_range_[item.first];
                ss << ", valid range [" << range.first << ", " << range.second
                   << ']';
            }
            if (option_enum_range_.count(item.first)) {
                ss << ", valid value [";
                const auto &set = option_enum_range_[item.first];
                for (auto p = set.begin(); p != set.end(); ++p) {
                    if (p != set.begin()) {
                        ss << ", ";
                    }
                    ss << *p;
                }
                ss << ']';
            }
            ss << ", current value [" << item.second(nullptr) << "]\n";
        }
        return ss.str();
    }

private:
    std::map<std::string, SetGetCallback> option_map_;
    std::map<std::string, std::pair<T, T>> option_min_max_range_;
    std::map<std::string, std::set<T>> option_enum_range_;
};

}  // namespace tmio
